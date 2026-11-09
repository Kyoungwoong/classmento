package com.classmento.interview.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import com.classmento.identity.domain.AppUser;
import com.classmento.interview.domain.Answer;
import com.classmento.interview.domain.AnswerRepository;
import com.classmento.interview.domain.AnswerStatus;
import com.classmento.interview.domain.InterviewLevel;
import com.classmento.interview.domain.InterviewType;
import com.classmento.interview.domain.JobRole;
import com.classmento.speech.SttClient;
import com.classmento.speech.Transcript;
import com.classmento.storage.FileStorage;
import com.classmento.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

/** STT가 도는 사이 다시 녹음하면, 늦게 끝난 예전 녹음의 결과는 버린다(audioVersion). */
class AnswerProcessorVersionTest extends IntegrationTest {

  @MockitoSpyBean SttClient stt;
  @Autowired InterviewService interviews;
  @Autowired AnswerRepository answers;
  @Autowired AnswerProcessor processor;
  @Autowired TransactionTemplate tx;
  @Autowired FileStorage storage;

  @Test
  void 처리_중에_다시_녹음하면_예전_결과를_버린다() {
    Long userId = createUser("me@test.com", AppUser.Role.USER).id();
    var session =
        interviews.create(
            userId,
            new InterviewService.Command(
                InterviewType.TECH, 3, InterviewLevel.JUNIOR, JobRole.BACKEND, null, null, false));
    String oldKey = storage.store("answers/test", "old.webm", new byte[] {1, 2, 3});
    Answer saved = answers.save(Answer.recorded(session, 1, oldKey, "audio/webm"));
    Long id = saved.getId();

    // STT가 도는 동안 사용자가 다시 녹음한다
    doAnswer(
            inv -> {
              tx.executeWithoutResult(
                  s -> answers.lockById(id).orElseThrow().reRecord("k-new", "audio/webm"));
              return new Transcript("예전 녹음의 글자", 0);
            })
        .when(stt)
        .transcribe(any(), anyString());

    processor.process(id, 1);

    Answer after = answers.findById(id).orElseThrow();
    assertThat(after.getAudioVersion()).isEqualTo(2);
    assertThat(after.getStatus()).isEqualTo(AnswerStatus.UPLOADED);
    assertThat(after.getTranscript()).isNull();
  }
}
