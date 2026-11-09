package com.classmento.interview.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classmento.common.domain.DomainException;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** 면접 끝내기: 모든 질문에 답한 뒤에만, 한 번만. 끝나면 답할 수 없고, 종합 리포트는 끝난 면접에만. */
class InterviewFinishTest {

  private static InterviewSession session() {
    return InterviewSession.start(
        7L,
        InterviewType.BEHAVIORAL,
        3,
        new Conditions(null, null, null, null, false),
        IntStream.range(0, 3)
            .mapToObj(
                i -> QuestionDraft.fromBank(100L + i, "질문 " + i, null, QuestionKind.BEHAVIORAL))
            .toList());
  }

  private static String code(Runnable r) {
    try {
      r.run();
    } catch (DomainException e) {
      return e.code();
    }
    throw new AssertionError("DomainException이 나야 해요");
  }

  @Test
  void 모든_질문에_답해야_끝낼_수_있다() {
    InterviewSession s = session();
    assertThat(code(() -> s.finish(Set.of(1, 2)))).isEqualTo("NOT_ALL_ANSWERED");
    s.finish(Set.of(1, 2, 3));
    assertThat(s.getStatus()).isEqualTo(SessionStatus.FINISHED);
  }

  @Test
  void 끝난_면접은_다시_끝낼_수_없고_답할_수도_없다() {
    InterviewSession s = session();
    s.finish(Set.of(1, 2, 3));
    assertThat(code(() -> s.finish(Set.of(1, 2, 3)))).isEqualTo("ALREADY_FINISHED");
    assertThat(code(() -> s.requireAnswerable(1))).isEqualTo("INTERVIEW_CLOSED");
  }

  @Test
  void 종합_리포트는_끝난_면접에만_한_번() {
    InterviewSession s = session();
    assertThat(code(() -> s.report(new SessionReport(3.5, "강점", "보완점", "p", "m"))))
        .isEqualTo("NOT_FINISHED");

    s.finish(Set.of(1, 2, 3));
    s.report(new SessionReport(3.5, "강점", "보완점", "p", "m"));
    assertThat(s.getStatus()).isEqualTo(SessionStatus.REPORTED);
    assertThat(s.getReport().overall()).isEqualTo(3.5);
    assertThat(s.getReport().strength()).isEqualTo("강점");
    assertThatThrownBy(() -> s.report(new SessionReport(1.0, "a", "b", "p", "m")))
        .isInstanceOf(DomainException.class);
  }

  @Test
  void 리포트가_없으면_null() {
    assertThat(session().getReport()).isNull();
  }
}
