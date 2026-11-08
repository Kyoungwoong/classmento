package com.classmento.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classmento.common.domain.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

  @Test
  void 앞뒤_공백을_지우고_소문자로_정규화한다() {
    assertThat(Email.of("  Kim.Test@Example.COM ").value()).isEqualTo("kim.test@example.com");
  }

  @Test
  void 같은_주소면_같은_값이다() {
    assertThat(Email.of("A@b.com")).isEqualTo(Email.of("a@B.com"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "abc", "a@", "@b.com", "a@b", "a b@c.com", "a@@b.com"})
  void 형식이_틀리면_도메인_예외(String raw) {
    assertThatThrownBy(() -> Email.of(raw))
        .isInstanceOf(DomainException.class)
        .hasMessage("이메일 형식이 맞지 않아요.")
        .extracting(e -> ((DomainException) e).code())
        .isEqualTo("INVALID_EMAIL");
  }

  @Test
  void 너무_길면_거부한다() {
    String local = "a".repeat(250);
    assertThatThrownBy(() -> Email.of(local + "@b.com")).isInstanceOf(DomainException.class);
  }
}
