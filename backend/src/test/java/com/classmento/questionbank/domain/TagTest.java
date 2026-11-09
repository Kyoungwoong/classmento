package com.classmento.questionbank.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classmento.common.domain.DomainException;
import org.junit.jupiter.api.Test;

/** 태그(Notion식): 이름 1~20자, 앞뒤·중복 공백 정리, 대소문자 무시로 같은 태그. */
class TagTest {

  @Test
  void 이름의_앞뒤_공백을_지우고_가운데_공백은_하나로() {
    assertThat(Tag.of("  Spring   Boot ").getName()).isEqualTo("Spring Boot");
  }

  @Test
  void 키는_대소문자를_무시한다() {
    assertThat(Tag.of("JPA").getKey()).isEqualTo(Tag.of("jpa").getKey());
  }

  @Test
  void 비거나_20자를_넘으면_만들_수_없다() {
    assertThatThrownBy(() -> Tag.of(" "))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).code())
        .isEqualTo("INVALID_TAG");
    assertThatThrownBy(() -> Tag.of("a".repeat(21))).isInstanceOf(DomainException.class);
  }

  @Test
  void 쉼표는_태그_구분자라_이름에_쓸_수_없다() {
    assertThatThrownBy(() -> Tag.of("a,b")).isInstanceOf(DomainException.class);
  }
}
