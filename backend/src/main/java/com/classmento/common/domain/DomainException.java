package com.classmento.common.domain;

/**
 * 도메인 규칙(불변식) 위반. 엔티티·값 객체가 던지고, 웹 계층에서 409로 변환된다. message는 사용자에게 그대로 보여줄 수 있는 한국어 문장. 프레임워크에 의존하지
 * 않는다(도메인 계층에서 사용).
 */
public class DomainException extends RuntimeException {

  private final String code;

  public DomainException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
