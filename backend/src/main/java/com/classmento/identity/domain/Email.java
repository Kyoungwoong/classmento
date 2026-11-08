package com.classmento.identity.domain;

import com.classmento.common.domain.DomainException;
import java.util.Locale;
import java.util.regex.Pattern;

/** 회원 이메일 값 객체. 생성 시 정규화(공백 제거·소문자)하고 형식을 검증한다. */
public record Email(String value) {

  private static final int MAX_LENGTH = 254;

  /** 로컬@도메인.최상위 — 공백·@ 중복 없이, 도메인에 점이 하나 이상. */
  private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

  public Email {
    if (value == null || value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
      throw new DomainException("INVALID_EMAIL", "이메일 형식이 맞지 않아요.");
    }
  }

  public static Email of(String raw) {
    return new Email(raw == null ? null : raw.strip().toLowerCase(Locale.ROOT));
  }
}
