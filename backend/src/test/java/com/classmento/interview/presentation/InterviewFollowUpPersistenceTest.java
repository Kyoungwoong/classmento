package com.classmento.interview.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classmento.common.security.AuthUser;
import com.classmento.identity.domain.AppUser;
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
import com.classmento.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/** 꼬리질문 저장(V10): 질문 목록을 통째로 다시 써도 행이 빠지지 않고, 꼬리가 붙은 답변은 다시 녹음할 수 없다. ADR 0008. */
class InterviewFollowUpPersistenceTest extends IntegrationTest {

  @Autowired InterviewSessionRepository sessions;
  @Autowired TransactionTemplate tx;

  private AuthUser me;

  @BeforeEach
  void setUp() {
    me = createUser("me@test.com", AppUser.Role.USER);
  }

  private long session(int maxFollowUps) {
    InterviewSession s =
        InterviewSession.start(
            me.id(),
            InterviewType.TECH,
            3,
            new Conditions(InterviewLevel.JUNIOR, JobRole.BACKEND, null, null, false),
            maxFollowUps,
            List.of(
                QuestionDraft.generated("캐시를 왜 썼나요?", "의도 1", QuestionKind.TECH),
                QuestionDraft.generated("인덱스는 어떻게 골랐나요?", "의도 2", QuestionKind.TECH),
                QuestionDraft.generated("배포는 어떻게 했나요?", "의도 3", QuestionKind.TECH)));
    return sessions.save(s).getId();
  }

  private void addFollowUp(long id, int parentSeq, String text) {
    tx.executeWithoutResult(
        t ->
            sessions
                .lockById(id)
                .orElseThrow()
                .addFollowUp(parentSeq, QuestionDraft.generated(text, "더 깊이", QuestionKind.TECH)));
  }

  private ResultActions answer(long id, int seq) throws Exception {
    return mockMvc.perform(
        multipart("/api/interviews/" + id + "/questions/" + seq + "/answer")
            .file(new MockMultipartFile("audio", "a.webm", "audio/webm", "webm".getBytes()))
            .with(csrf())
            .with(user(me)));
  }

  @Test
  void 꼬리를_붙이고_닫아도_모든_질문이_순서대로_남는다() {
    long id = session(2);
    addFollowUp(id, 1, "무효화는 어떻게 했나요?");
    addFollowUp(id, 4, "만료 시간은 어떻게 정했나요?");
    tx.executeWithoutResult(t -> sessions.lockById(id).orElseThrow().closeFollowUp(2));

    InterviewSession s = sessions.findById(id).orElseThrow();
    assertThat(s.getMaxFollowUps()).isEqualTo(2);
    assertThat(s.getQuestions())
        .extracting(InterviewQuestion::getSeq)
        .containsExactly(1, 4, 5, 2, 3);
    assertThat(s.getQuestions())
        .extracting(InterviewQuestion::getFollowUp)
        .containsExactly(
            FollowUpState.ASKED,
            FollowUpState.ASKED,
            FollowUpState.NONE,
            FollowUpState.CLOSED,
            FollowUpState.AWAITING);
    assertThat(s.question(5).getParentSeq()).isEqualTo(4);
    assertThat(s.question(5).getRootSeq()).isEqualTo(1);
    assertThat(s.rootCount()).isEqualTo(3);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM interview_question WHERE session_id = ?", Integer.class, id))
        .isEqualTo(5);
  }

  @Test
  void 꼬리가_붙은_답변은_다시_녹음할_수_없다() throws Exception {
    long id = session(1);
    answer(id, 1).andExpect(status().isAccepted()); // 이 답으로 꼬리질문이 붙는다(가짜 LLM)
    assertThat(sessions.findById(id).orElseThrow().question(1).getFollowUp())
        .isEqualTo(FollowUpState.ASKED);

    answer(id, 1)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("FOLLOW_UP_ASKED"));
    answer(id, 2).andExpect(status().isAccepted()); // 같은 꼬리 문장이라 닫힘
    answer(id, 2).andExpect(status().isAccepted()); // 꼬리가 없으면 다시 녹음할 수 있다
  }
}
