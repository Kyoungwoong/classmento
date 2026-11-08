package com.classmento.common.web;

import org.springframework.http.HttpStatus;

/** 도메인에서 던지는 예외. {@link GlobalExceptionHandler}가 {@link ApiError}로 바꾼다. message는 사용자용 한국어. */
public class ApiException extends RuntimeException {

  private final HttpStatus status;
  private final String code;

  public ApiException(HttpStatus status, String code, String message) {
    super(message);
    this.status = status;
    this.code = code;
  }

  public static ApiException notFound() {
    return new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "찾을 수 없어요.");
  }

  public static ApiException badRequest(String message) {
    return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", message);
  }

  public HttpStatus status() {
    return status;
  }

  public String code() {
    return code;
  }
}
