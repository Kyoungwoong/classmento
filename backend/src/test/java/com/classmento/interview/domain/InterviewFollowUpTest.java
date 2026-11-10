package com.classmento.interview.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.classmento.common.domain.DomainException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** 꼬리질문: 새 면접 옵션(없음·한 번·두 번), 인성 제외, 답을 듣고 뒤에 붙인다. ADR 0008. */
class InterviewFollowUpTest {

  private static final Conditions TECH_COND =
      new Conditions(InterviewLevel.JUNIOR, JobRole.BACKEND, null, null, false);

  private static List<QuestionDraft> drafts(QuestionKind... kinds) {
    return IntStream.range(0, kinds.length)
        .mapToObj(i -> QuestionDraft.fromBank(100L + i, "질문 " + (i + 1), "의도", kinds[i]))
        .toList();
  }

  private static InterviewSession tech(int maxFollowUps) {
    return InterviewSession.start(
        7L,
        InterviewType.TECH,
        3,
        TECH_COND,
        maxFollowUps,
        drafts(QuestionKind.TECH, QuestionKind.TECH, QuestionKind.TECH));
  }

  private static QuestionDraft follow(String text) {
    return QuestionDraft.generated(text, "더 깊이", QuestionKind.TECH);
  }

  private static String code(Runnable r) {
    try {
      r.run();
    } catch (DomainException e) {
      return e.code();
    }
    throw new AssertionError("DomainException이 나야 해요");
  }

  private static Map<Integer, AnswerStatus> answered(InterviewSession s) {
    Map<Integer, AnswerStatus> m = new HashMap<>();
    s.getQuestions().forEach(q -> m.put(q.getSeq(), AnswerStatus.TRANSCRIBED));
    return m;
  }

  @Test
  void 꼬리질문_수는_0에서_2까지이고_인성_면접에는_켤_수_없다() {
    assertThat(code(() -> tech(3))).isEqualTo("INVALID_FOLLOW_UPS");
    assertThat(code(() -> tech(-1))).isEqualTo("INVALID_FOLLOW_UPS");
    assertThat(
            code(
                () ->
                    InterviewSession.start(
                        7L,
                        InterviewType.BEHAVIORAL,
                        3,
                        new Conditions(null, null, null, null, false),
                        1,
                        drafts(
                            QuestionKind.BEHAVIORAL,
                            QuestionKind.BEHAVIORAL,
                            QuestionKind.BEHAVIORAL))))
        .isEqualTo("FOLLOW_UPS_NOT_ALLOWED");
  }

  @Test
  void 옵션이_없으면_모든_질문이_꼬리_대상이_아니다() {
    InterviewSession s = tech(0);
    assertThat(s.getMaxFollowUps()).isZero();
    assertThat(s.getQuestions())
        .allSatisfy(
            q -> {
              assertThat(q.getFollowUp()).isEqualTo(FollowUpState.NONE);
              assertThat(q.getDepth()).isZero();
              assertThat(q.getParentSeq()).isNull();
              assertThat(q.getRootSeq()).isEqualTo(q.getSeq());
            });
  }

  @Test
  void 혼합_면접은_기술_질문에만_꼬리를_기다린다() {
    InterviewSession s =
        InterviewSession.start(
            7L,
            InterviewType.MIXED,
            3,
            TECH_COND,
            1,
            drafts(QuestionKind.TECH, QuestionKind.BEHAVIORAL, QuestionKind.TECH));
    assertThat(s.getQuestions())
        .extracting(InterviewQuestion::getFollowUp)
        .containsExactly(FollowUpState.AWAITING, FollowUpState.NONE, FollowUpState.AWAITING);
  }

  @Test
  void 꼬리는_뒤_번호로_붙고_화면_순서는_본_질문_바로_다음이다() {
    InterviewSession s = tech(2);
    InterviewQuestion f1 = s.addFollowUp(1, follow("그 캐시는 어떻게 무효화했나요?"));

    assertThat(f1.getSeq()).isEqualTo(4);
    assertThat(f1.getParentSeq()).isEqualTo(1);
    assertThat(f1.getRootSeq()).isEqualTo(1);
    assertThat(f1.getDepth()).isEqualTo(1);
    assertThat(f1.getFollowUp()).isEqualTo(FollowUpState.AWAITING); // 두 번까지라 한 번 더 기다린다
    assertThat(s.question(1).getFollowUp()).isEqualTo(FollowUpState.ASKED);

    InterviewQuestion f2 = s.addFollowUp(4, follow("만료 시간은 어떻게 정했나요?"));
    assertThat(f2.getSeq()).isEqualTo(5);
    assertThat(f2.getRootSeq()).isEqualTo(1);
    assertThat(f2.getDepth()).isEqualTo(2);
    assertThat(f2.getFollowUp()).isEqualTo(FollowUpState.NONE); // 최대 깊이

    assertThat(s.getQuestions())
        .extracting(InterviewQuestion::getSeq)
        .containsExactly(1, 4, 5, 2, 3);
    assertThat(s.rootCount()).isEqualTo(3);
  }

  @Test
  void 이미_정한_질문에는_꼬리를_다시_붙이거나_닫을_수_없다() {
    InterviewSession s = tech(1);
    s.addFollowUp(1, follow("왜 그 방식을 골랐나요?"));
    assertThat(code(() -> s.addFollowUp(1, follow("다른 꼬리")))).isEqualTo("FOLLOW_UP_DECIDED");
    s.closeFollowUp(1); // 이미 ASKED면 아무 일도 없다(같은 결과가 와도 안전)
    assertThat(s.question(1).getFollowUp()).isEqualTo(FollowUpState.ASKED);

    s.closeFollowUp(2);
    assertThat(s.question(2).getFollowUp()).isEqualTo(FollowUpState.CLOSED);
    assertThat(code(() -> s.addFollowUp(2, follow("늦게 온 꼬리")))).isEqualTo("FOLLOW_UP_DECIDED");
  }

  @Test
  void 꼬리는_기술이어야_하고_같은_문장은_안_되고_끝난_면접에는_붙지_않는다() {
    InterviewSession s = tech(1);
    assertThat(
            code(
                () ->
                    s.addFollowUp(
                        1, QuestionDraft.generated("협업은요?", null, QuestionKind.BEHAVIORAL))))
        .isEqualTo("INVALID_FOLLOW_UP");
    assertThat(code(() -> s.addFollowUp(1, follow("  질문   2 ")))).isEqualTo("DUPLICATE_QUESTION");

    s.closeFollowUp(1);
    s.closeFollowUp(2);
    s.closeFollowUp(3);
    s.finish(answered(s));
    assertThat(code(() -> s.addFollowUp(1, follow("끝난 뒤 꼬리")))).isEqualTo("INTERVIEW_CLOSED");
  }

  @Test
  void 꼬리를_정하는_중이면_끝낼_수_없고_정해지면_꼬리까지_답해야_끝난다() {
    InterviewSession s = tech(1);
    assertThat(s.awaitingFollowUps(answered(s))).containsExactly(1, 2, 3);
    assertThat(code(() -> s.finish(answered(s)))).isEqualTo("FOLLOW_UP_PENDING");

    s.addFollowUp(1, follow("어떻게 측정했나요?"));
    s.closeFollowUp(2);
    s.closeFollowUp(3);
    Map<Integer, AnswerStatus> noFollowUpAnswer =
        new HashMap<>(
            Map.of(
                1,
                AnswerStatus.TRANSCRIBED,
                2,
                AnswerStatus.TRANSCRIBED,
                3,
                AnswerStatus.TRANSCRIBED));
    assertThat(code(() -> s.finish(noFollowUpAnswer))).isEqualTo("NOT_ALL_ANSWERED");

    s.finish(answered(s));
    assertThat(s.getStatus()).isEqualTo(SessionStatus.FINISHED);
  }

  @Test
  void 답이_없거나_실패한_질문은_꼬리를_기다리는_중으로_치지_않는다() {
    InterviewSession s = tech(1);
    Map<Integer, AnswerStatus> m =
        new HashMap<>(Map.of(1, AnswerStatus.FAILED, 2, AnswerStatus.EVALUATED));
    assertThat(s.awaitingFollowUps(m)).containsExactly(2);
  }

  @Test
  void 꼬리가_붙은_본_질문은_다시_녹음할_수_없다() {
    InterviewSession s = tech(1);
    s.requireReRecordable(1);
    s.addFollowUp(1, follow("그때 병목은 어디였나요?"));
    assertThat(code(() -> s.requireReRecordable(1))).isEqualTo("FOLLOW_UP_ASKED");
    s.requireReRecordable(4);
  }
}
