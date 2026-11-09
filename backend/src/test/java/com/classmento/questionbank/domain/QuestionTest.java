package com.classmento.questionbank.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classmento.common.domain.DomainException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

/** 질문 규칙: 소분류에만, 숨긴 카테고리 금지, 질문 5~300자, 직무 1개 이상, 태그 최대 10개. */
class QuestionTest {

  private static String code(Throwable e) {
    return ((DomainException) e).code();
  }

  private static Category top() {
    Category top = Category.top("CS", 0);
    top.assignIdForTest(1L);
    return top;
  }

  private static Category sub(Category top) {
    Category sub = Category.child(top, "네트워크", 0);
    sub.assignIdForTest(2L);
    return sub;
  }

  private static Question valid(Category top, Category sub) {
    return Question.create(
        top,
        sub,
        "  TCP와 UDP의 차이를 설명해 주세요.  ",
        Level.JUNIOR,
        Set.of(JobRole.BACKEND),
        Set.of(10L),
        "전송 계층 이해",
        7L);
  }

  @Test
  void 소분류에_질문을_만들고_앞뒤_공백을_지운다() {
    Category top = top();
    Question q = valid(top, sub(top));
    assertThat(q.getCategoryId()).isEqualTo(2L);
    assertThat(q.getText()).isEqualTo("TCP와 UDP의 차이를 설명해 주세요.");
    assertThat(q.getJobRoles()).containsExactly(JobRole.BACKEND);
    assertThat(q.getTagIds()).containsExactly(10L);
    assertThat(q.isActive()).isTrue();
    assertThat(q.getCreatedBy()).isEqualTo(7L);
  }

  @Test
  void 대분류에는_질문을_붙일_수_없다() {
    Category top = top();
    assertThatThrownBy(() -> valid(top, top))
        .extracting(QuestionTest::code)
        .isEqualTo("QUESTION_NEEDS_SUBCATEGORY");
  }

  @Test
  void 소분류와_대분류가_맞지_않으면_만들_수_없다() {
    Category top = top();
    Category otherTop = Category.top("Language", 1);
    otherTop.assignIdForTest(9L);
    assertThatThrownBy(() -> valid(otherTop, sub(top)))
        .extracting(QuestionTest::code)
        .isEqualTo("QUESTION_NEEDS_SUBCATEGORY");
  }

  @Test
  void 숨긴_소분류나_숨긴_대분류에는_새_질문_금지() {
    Category top = top();
    Category sub = sub(top);
    sub.hide();
    assertThatThrownBy(() -> valid(top, sub))
        .extracting(QuestionTest::code)
        .isEqualTo("CATEGORY_HIDDEN");

    Category top2 = top();
    Category sub2 = sub(top2);
    top2.hide();
    assertThatThrownBy(() -> valid(top2, sub2))
        .extracting(QuestionTest::code)
        .isEqualTo("CATEGORY_HIDDEN");
  }

  @Test
  void 질문은_5자에서_300자() {
    Category top = top();
    Category sub = sub(top);
    for (String text : new String[] {"짧아", "가".repeat(301), null}) {
      assertThatThrownBy(
              () ->
                  Question.create(
                      top, sub, text, Level.NEW, Set.of(JobRole.COMMON), Set.of(), null, null))
          .extracting(QuestionTest::code)
          .isEqualTo("INVALID_QUESTION");
    }
  }

  @Test
  void 직무는_1개_이상() {
    Category top = top();
    Category sub = sub(top);
    assertThatThrownBy(
            () -> Question.create(top, sub, "질문입니다 질문", Level.NEW, Set.of(), Set.of(), null, null))
        .extracting(QuestionTest::code)
        .isEqualTo("QUESTION_JOB_ROLE_REQUIRED");
  }

  @Test
  void 태그는_최대_10개() {
    Category top = top();
    Category sub = sub(top);
    Set<Long> eleven = LongStream.rangeClosed(1, 11).boxed().collect(Collectors.toSet());
    assertThatThrownBy(
            () ->
                Question.create(
                    top, sub, "질문입니다 질문", Level.NEW, Set.of(JobRole.COMMON), eleven, null, null))
        .extracting(QuestionTest::code)
        .isEqualTo("TOO_MANY_TAGS");
  }

  @Test
  void 의도는_비면_null() {
    Category top = top();
    Question q =
        Question.create(
            top, sub(top), "질문입니다 질문", Level.NEW, Set.of(JobRole.COMMON), Set.of(), "  ", null);
    assertThat(q.getIntent()).isNull();
  }

  @Test
  void 수정도_같은_규칙이고_숨기고_다시_보인다() {
    Category top = top();
    Category sub = sub(top);
    Question q = valid(top, sub);

    q.edit(top, sub, "HTTP/2의 특징은요?", Level.MIDDLE, Set.of(JobRole.FRONTEND), Set.of(), null);
    assertThat(q.getText()).isEqualTo("HTTP/2의 특징은요?");
    assertThat(q.getLevel()).isEqualTo(Level.MIDDLE);
    assertThat(q.getTagIds()).isEmpty();

    assertThatThrownBy(
            () -> q.edit(top, sub, "짧", Level.NEW, Set.of(JobRole.COMMON), Set.of(), null))
        .extracting(QuestionTest::code)
        .isEqualTo("INVALID_QUESTION");

    q.hide();
    assertThat(q.isActive()).isFalse();
    q.show();
    assertThat(q.isActive()).isTrue();
  }

  @Test
  void 같은_숨긴_카테고리_안에서는_수정할_수_있지만_숨긴_카테고리로_옮길_수는_없다() {
    Category top = top();
    Category sub = sub(top);
    Question q = valid(top, sub);
    sub.hide();
    q.edit(top, sub, "숨긴 카테고리 안의 질문 수정", Level.NEW, Set.of(JobRole.COMMON), Set.of(), null);

    Category hiddenOther = Category.child(top, "보안", 1);
    hiddenOther.assignIdForTest(3L);
    hiddenOther.hide();
    assertThatThrownBy(
            () ->
                q.edit(
                    top,
                    hiddenOther,
                    "다른 숨긴 곳으로",
                    Level.NEW,
                    Set.of(JobRole.COMMON),
                    Set.of(),
                    null))
        .extracting(QuestionTest::code)
        .isEqualTo("CATEGORY_HIDDEN");
  }
}
