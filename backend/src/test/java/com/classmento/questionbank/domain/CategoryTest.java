package com.classmento.questionbank.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classmento.common.domain.DomainException;
import org.junit.jupiter.api.Test;

/** 카테고리는 대분류 → 소분류 2단계. 이름 1~30자, 삭제 대신 숨김. */
class CategoryTest {

  private static String code(Throwable e) {
    return ((DomainException) e).code();
  }

  /** 저장된 것처럼 id가 있는 대분류(소분류 생성 시 부모 id가 필요). */
  private static Category savedTop(String name) {
    Category top = Category.top(name, 0);
    top.assignIdForTest(1L);
    return top;
  }

  @Test
  void 대분류는_부모가_없고_이름은_앞뒤_공백을_지운다() {
    Category top = Category.top("  CS  ", 0);
    assertThat(top.getName()).isEqualTo("CS");
    assertThat(top.isTop()).isTrue();
    assertThat(top.isActive()).isTrue();
  }

  @Test
  void 소분류는_대분류_아래에만_만들_수_있다() {
    Category top = savedTop("CS");
    Category child = Category.child(top, "자료구조", 0);
    assertThat(child.getParentId()).isEqualTo(1L);
    assertThat(child.isTop()).isFalse();

    Category grandChild = child;
    assertThatThrownBy(() -> Category.child(grandChild, "트리", 0))
        .isInstanceOf(DomainException.class)
        .extracting(CategoryTest::code)
        .isEqualTo("CATEGORY_TOO_DEEP");
  }

  @Test
  void 숨긴_대분류_아래에는_소분류를_만들_수_없다() {
    Category top = savedTop("CS");
    top.hide();
    assertThatThrownBy(() -> Category.child(top, "자료구조", 0))
        .extracting(CategoryTest::code)
        .isEqualTo("CATEGORY_HIDDEN");
  }

  @Test
  void 이름이_비거나_30자를_넘으면_만들_수_없다() {
    assertThatThrownBy(() -> Category.top("  ", 0))
        .extracting(CategoryTest::code)
        .isEqualTo("INVALID_CATEGORY");
    assertThatThrownBy(() -> Category.top("가".repeat(31), 0))
        .extracting(CategoryTest::code)
        .isEqualTo("INVALID_CATEGORY");
  }

  @Test
  void 이름을_바꿀_때도_같은_규칙() {
    Category top = Category.top("CS", 0);
    top.rename(" 컴퓨터 과학 ");
    assertThat(top.getName()).isEqualTo("컴퓨터 과학");
    assertThatThrownBy(() -> top.rename(""))
        .extracting(CategoryTest::code)
        .isEqualTo("INVALID_CATEGORY");
  }

  @Test
  void 중복_판단용_키는_대소문자와_공백을_무시한다() {
    assertThat(Category.nameKey(" Java Script ")).isEqualTo(Category.nameKey("javascript"));
  }

  /** DB 유니크 인덱스(Postgres \s, 유니코드 공백 포함)와 같은 기준이어야 서비스 검사를 지나 DB에서 막히는 일이 없다. */
  @Test
  void 중복_판단용_키는_유니코드_공백도_무시한다() {
    assertThat(Category.nameKey("Java Script　")).isEqualTo(Category.nameKey("javascript"));
  }

  @Test
  void 숨기고_다시_보일_수_있다() {
    Category top = Category.top("CS", 0);
    top.hide();
    assertThat(top.isActive()).isFalse();
    top.show();
    assertThat(top.isActive()).isTrue();
  }
}
