package com.classmento.interview.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classmento.common.security.AuthUser;
import com.classmento.identity.domain.AppUser;
import com.classmento.interview.application.AnswerService;
import com.classmento.interview.domain.AnswerStatus;
import com.classmento.interview.domain.Conditions;
import com.classmento.interview.domain.FollowUpState;
import com.classmento.interview.domain.InterviewLevel;
import com.classmento.interview.domain.InterviewQuestion;
import com.classmento.interview.domain.InterviewSession;
import com.classmento.interview.domain.InterviewSessionRepository;
import com.classmento.interview.domain.InterviewType;
import com.classmento.interview.domain.JobRole;
import com.classmento.interview.domain.QuestionDraft;
import com.classmento.interview.domain.QuestionKind;
import com.classmento.llm.LlmClient;
import com.classmento.speech.SttClient;
import com.classmento.speech.Transcript;
import com.classmento.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 꼬리질문 만들기(테스트: 동기 실행, 가짜 STT·LLM). 가짜 LLM은 늘 같은 꼬리질문을 돌려주므로, 두 번째부터는 같은 문장이라 받지 않고 닫힌다. ADR 0008.
 */
class InterviewFollowUpGenerationTest extends IntegrationTest {

  @Autowired InterviewSessionRepository sessions;
  @Autowired AnswerService answerService;
  @MockitoSpyBean LlmClient llm;
  @MockitoSpyBean SttClient stt;

  private AuthUser me;

  @BeforeEach
  void setUp() {
    me = createUser("me@test.com", AppUser.Role.USER);
  }

  private long session(InterviewType type, int maxFollowUps, QuestionKind... kinds) {
    List<QuestionDraft> drafts =
        java.util.stream.IntStream.range(0, kinds.length)
            .mapToObj(i -> QuestionDraft.generated("본 질문 " + (i + 1) + "번이에요?", "의도", kinds[i]))
            .toList();
    InterviewSession s =
        InterviewSession.start(
            me.id(),
            type,
            kinds.length,
            new Conditions(InterviewLevel.JUNIOR, JobRole.BACKEND, null, null, false),
            maxFollowUps,
            drafts);
    return sessions.save(s).getId();
  }

  private ResultActions answer(long id, int seq) throws Exception {
    return mockMvc.perform(
        multipart("/api/interviews/" + id + "/questions/" + seq + "/answer")
            .file(new MockMultipartFile("audio", "a.webm", "audio/webm", "webm".getBytes()))
            .with(csrf())
            .with(user(me)));
  }

  private InterviewSession reload(long id) {
    return sessions.findById(id).orElseThrow();
  }

  @Test
  void 답을_올리면_평가보다_먼저_그_답에서_꼬리질문을_만들어_본_질문_바로_뒤에_둔다() throws Exception {
    long id =
        session(InterviewType.TECH, 2, QuestionKind.TECH, QuestionKind.TECH, QuestionKind.TECH);

    answer(id, 1).andExpect(status().isAccepted());

    InterviewSession s = reload(id);
    assertThat(s.getQuestions()).extracting(InterviewQuestion::getSeq).containsExactly(1, 4, 2, 3);
    assertThat(s.question(1).getFollowUp()).isEqualTo(FollowUpState.ASKED);
    InterviewQuestion f = s.question(4);
    assertThat(f.getParentSeq()).isEqualTo(1);
    assertThat(f.getDepth()).isEqualTo(1);
    assertThat(f.getText()).contains("가짜 꼬리질문");
    assertThat(f.getFollowUp()).isEqualTo(FollowUpState.AWAITING); // 두 번까지

    // 꼬리에 답하면: 가짜 LLM이 같은 문장을 다시 내므로 받지 않고 닫는다(면접은 계속)
    answer(id, 4).andExpect(status().isAccepted());
    assertThat(reload(id).question(4).getFollowUp()).isEqualTo(FollowUpState.CLOSED);
  }

  @Test
  void 꼬리까지_모두_답하면_끝내고_리포트에_꼬리_답변도_들어간다() throws Exception {
    long id =
        session(InterviewType.TECH, 1, QuestionKind.TECH, QuestionKind.TECH, QuestionKind.TECH);
    answer(id, 1);
    answer(id, 2); // 같은 꼬리 문장 → 닫힘
    answer(id, 3);
    mockMvc
        .perform(post("/api/interviews/" + id + "/finish").with(csrf()).with(user(me)))
        .andExpect(jsonPath("$.code").value("NOT_ALL_ANSWERED"));

    answer(id, 4);
    mockMvc
        .perform(post("/api/interviews/" + id + "/finish").with(csrf()).with(user(me)))
        .andExpect(status().isAccepted());
    mockMvc
        .perform(get("/api/interviews/" + id + "/report").with(user(me)))
        .andExpect(jsonPath("$.status").value("READY"))
        .andExpect(jsonPath("$.items.length()").value(4));
  }

  @Test
  void 혼합_면접은_평가를_미룬_채로_기술_질문에만_꼬리를_만든다() throws Exception {
    long id =
        session(
            InterviewType.MIXED, 1, QuestionKind.TECH, QuestionKind.BEHAVIORAL, QuestionKind.TECH);
    answer(id, 1);
    answer(id, 2);

    InterviewSession s = reload(id);
    assertThat(s.question(1).getFollowUp()).isEqualTo(FollowUpState.ASKED);
    assertThat(s.question(2).getFollowUp()).isEqualTo(FollowUpState.NONE);
    assertThat(
            jdbc.queryForObject(
                "SELECT status FROM interview_answer WHERE session_id = ? AND seq = 1",
                String.class,
                id))
        .isEqualTo(AnswerStatus.TRANSCRIBED.name());
  }

  @Test
  void 짧은_답이면_AI를_부르지_않고_꼬리를_닫는다() throws Exception {
    doReturn(new Transcript("잘 모르겠습니다.", 2000)).when(stt).transcribe(any(), anyString());
    long id =
        session(InterviewType.TECH, 1, QuestionKind.TECH, QuestionKind.TECH, QuestionKind.TECH);

    answer(id, 1);

    assertThat(reload(id).question(1).getFollowUp()).isEqualTo(FollowUpState.CLOSED);
    verify(llm, never())
        .generate(argThat(r -> r != null && r.promptName().startsWith("follow-up-question")));
  }

  @Test
  void 중간에_끊겨_정하지_못한_꼬리는_면접을_다시_불러올_때_정한다() throws Exception {
    long id =
        session(InterviewType.TECH, 1, QuestionKind.TECH, QuestionKind.TECH, QuestionKind.TECH);
    answer(id, 2); // 2번은 꼬리 생성(닫힘이 아니라 ASKED)
    // 서버가 꼬리를 정하던 중 멈춘 상황: 답은 있는데 질문은 아직 AWAITING
    jdbc.update(
        "UPDATE interview_question SET follow_up = 'AWAITING' WHERE session_id = ? AND seq = 2",
        id);
    jdbc.update("DELETE FROM interview_question WHERE session_id = ? AND seq = 4", id);

    mockMvc.perform(get("/api/interviews/" + id).with(user(me))).andExpect(status().isOk());

    InterviewSession s = reload(id);
    assertThat(s.question(2).getFollowUp()).isEqualTo(FollowUpState.ASKED);
    assertThat(s.getQuestions()).extracting(InterviewQuestion::getSeq).containsExactly(1, 2, 4, 3);
  }

  @Test
  void 꼬리를_정하는_중이면_끝내기를_미루고_다시_정하게_한다() throws Exception {
    long id =
        session(InterviewType.TECH, 1, QuestionKind.TECH, QuestionKind.TECH, QuestionKind.TECH);
    answer(id, 1);
    answer(id, 4);
    answer(id, 2);
    answer(id, 3);
    jdbc.update(
        "UPDATE interview_question SET follow_up = 'AWAITING' WHERE session_id = ? AND seq = 3",
        id);

    mockMvc
        .perform(post("/api/interviews/" + id + "/finish").with(csrf()).with(user(me)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("FOLLOW_UP_PENDING"));

    // 거절하면서 다시 정하게 했다: 같은 꼬리 문장이라 닫힌다 → 이제 끝낼 수 있다
    assertThat(reload(id).question(3).getFollowUp()).isEqualTo(FollowUpState.CLOSED);
    mockMvc
        .perform(post("/api/interviews/" + id + "/finish").with(csrf()).with(user(me)))
        .andExpect(status().isAccepted());
  }

  @Test
  void 꼬리를_정하는_사이_다시_녹음하면_옛_답으로_만든_꼬리는_버리고_다시_정한다() throws Exception {
    long id =
        session(InterviewType.TECH, 1, QuestionKind.TECH, QuestionKind.TECH, QuestionKind.TECH);
    boolean[] reRecorded = {false};
    doAnswer(
            inv -> {
              if (!reRecorded[0]) {
                reRecorded[0] = true; // 옛 답(1번째 녹음)으로 꼬리를 만드는 도중 다시 녹음
                answerService.upload(me.id(), id, 1, "webm-2".getBytes(), "audio/webm");
              }
              return inv.callRealMethod();
            })
        .when(llm)
        .generate(argThat(r -> r != null && r.promptName().startsWith("follow-up-question")));

    answer(id, 1);

    InterviewSession s = reload(id);
    assertThat(s.getQuestions()).hasSize(3); // 옛 답의 꼬리는 버렸다
    assertThat(s.question(1).getFollowUp()).isEqualTo(FollowUpState.AWAITING);

    // 진행 화면이 다시 불러오면 새 답으로 다시 정한다
    mockMvc.perform(get("/api/interviews/" + id).with(user(me))).andExpect(status().isOk());
    assertThat(reload(id).question(1).getFollowUp()).isEqualTo(FollowUpState.ASKED);
  }
}
