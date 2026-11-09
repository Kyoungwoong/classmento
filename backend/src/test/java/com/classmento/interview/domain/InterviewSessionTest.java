package com.classmento.interview.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classmento.common.domain.DomainException;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** 면접 세션 규칙: 질문 수 3·5·7, 탭별 필수 조건, 질문은 정해진 수만큼·같은 은행 질문 한 번만. */
class InterviewSessionTest {

  private static final Conditions TECH =
      new Conditions(InterviewLevel.JUNIOR, JobRole.BACKEND, null, null, false);

  private static List<QuestionDraft> drafts(int n) {
    return IntStream.range(0, n)
        .mapToObj(i -> QuestionDraft.fromBank(100L + i, "질문 " + i, "의도", QuestionKind.TECH))
        .toList();
  }

  private static DomainException code(Runnable r) {
    try {
      r.run();
    } catch (DomainException e) {
      return e;
    }
    throw new AssertionError("DomainException이 나야 해요");
  }

  @Test
  void 질문_수는_3_5_7만() {
    InterviewSession.requireValid(InterviewType.TECH, 5, TECH);
    assertThat(code(() -> InterviewSession.requireValid(InterviewType.TECH, 4, TECH)).code())
        .isEqualTo("INVALID_QUESTION_COUNT");
  }

  @Test
  void 경험_기반은_이력서와_경험이_필요하다() {
    Conditions none = new Conditions(null, null, null, null, false);
    assertThat(code(() -> InterviewSession.requireValid(InterviewType.EXPERIENCE, 3, none)).code())
        .isEqualTo("EXPERIENCE_REQUIRED");
    InterviewSession.requireValid(
        InterviewType.EXPERIENCE, 3, new Conditions(null, null, 1L, 2L, false));
  }

  @Test
  void 기술과_혼합은_직무와_연차가_필요하고_인성은_없어도_된다() {
    Conditions none = new Conditions(null, null, null, null, false);
    assertThat(code(() -> InterviewSession.requireValid(InterviewType.TECH, 3, none)).code())
        .isEqualTo("CONDITIONS_REQUIRED");
    assertThat(code(() -> InterviewSession.requireValid(InterviewType.MIXED, 3, none)).code())
        .isEqualTo("CONDITIONS_REQUIRED");
    InterviewSession.requireValid(InterviewType.BEHAVIORAL, 3, none);
  }

  @Test
  void 이력서로_좁히려면_이력서가_필요하다() {
    Conditions narrow = new Conditions(InterviewLevel.JUNIOR, JobRole.BACKEND, null, null, true);
    assertThat(code(() -> InterviewSession.requireValid(InterviewType.TECH, 3, narrow)).code())
        .isEqualTo("RESUME_REQUIRED");
  }

  @Test
  void 시작하면_질문에_1부터_번호를_붙이고_진행_중이다() {
    InterviewSession s = InterviewSession.start(7L, InterviewType.TECH, 3, TECH, drafts(3));

    assertThat(s.getUserId()).isEqualTo(7L);
    assertThat(s.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
    assertThat(s.getQuestions()).extracting(InterviewQuestion::getSeq).containsExactly(1, 2, 3);
    assertThat(s.getQuestions().get(0).getBankQuestionId()).isEqualTo(100L);
  }

  @Test
  void 질문이_모자라면_시작할_수_없다() {
    assertThat(
            code(() -> InterviewSession.start(7L, InterviewType.TECH, 5, TECH, drafts(4))).code())
        .isEqualTo("NOT_ENOUGH_QUESTIONS");
  }

  @Test
  void 같은_은행_질문은_한_번만() {
    List<QuestionDraft> dup =
        List.of(
            QuestionDraft.fromBank(1L, "질문 a", null, QuestionKind.TECH),
            QuestionDraft.fromBank(1L, "질문 a", null, QuestionKind.TECH),
            QuestionDraft.generated("만든 질문", "의도", QuestionKind.TECH));
    assertThatThrownBy(() -> InterviewSession.start(7L, InterviewType.TECH, 3, TECH, dup))
        .isInstanceOf(DomainException.class)
        .extracting("code")
        .isEqualTo("DUPLICATE_QUESTION");
  }

  @Test
  void 탭이_쓰지_않는_조건은_저장하지_않는다() {
    Conditions all = new Conditions(InterviewLevel.JUNIOR, JobRole.BACKEND, 5L, 6L, false);

    InterviewSession behavioral =
        InterviewSession.start(7L, InterviewType.BEHAVIORAL, 3, all, drafts(3));
    assertThat(behavioral.getResumeId()).isNull();
    assertThat(behavioral.getExperienceId()).isNull();
    assertThat(behavioral.getLevel()).isNull();

    InterviewSession techNoNarrow =
        InterviewSession.start(7L, InterviewType.TECH, 3, all, drafts(3));
    assertThat(techNoNarrow.getResumeId()).isNull(); // 좁히지 않으면 이력서를 쓰지 않는다
    assertThat(techNoNarrow.getLevel()).isEqualTo(InterviewLevel.JUNIOR);

    Conditions narrow = new Conditions(InterviewLevel.JUNIOR, JobRole.BACKEND, 5L, 6L, true);
    InterviewSession techNarrow =
        InterviewSession.start(7L, InterviewType.TECH, 3, narrow, drafts(3));
    assertThat(techNarrow.getResumeId()).isEqualTo(5L);
    assertThat(techNarrow.getExperienceId()).isNull();
  }

  @Test
  void 혼합은_이력서로_좁히지_않는다() {
    Conditions narrow = new Conditions(InterviewLevel.JUNIOR, JobRole.BACKEND, null, null, true);
    InterviewSession.requireValid(InterviewType.MIXED, 3, narrow); // RESUME_REQUIRED 아님
  }

  @Test
  void 만든_질문은_은행_번호가_없다() {
    QuestionDraft g =
        QuestionDraft.generated("이 프로젝트에서 Redis를 고른 이유는요?", "선택 근거", QuestionKind.TECH);
    assertThat(g.bankQuestionId()).isNull();
    assertThat(g.generated()).isTrue();
  }
}
