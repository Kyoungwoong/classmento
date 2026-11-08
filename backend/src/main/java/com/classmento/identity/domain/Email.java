package com.classmento.identity.domain;

/** 회원 이메일 값 객체. 생성 시 정규화(공백 제거·소문자)하고 형식을 검증한다. */
public record Email(String value) {

  public static Email of(String raw) {
    throw new UnsupportedOperationException("TODO");
  }
}
