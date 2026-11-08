package com.classmento.common.api;

import com.classmento.llm.LlmUnavailableException;
import com.classmento.speech.SttUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
    return ResponseEntity.badRequest().body(new ApiError("INVALID_REQUEST", "입력한 내용을 다시 확인해 주세요."));
  }

  @ExceptionHandler({LlmUnavailableException.class, SttUnavailableException.class})
  public ResponseEntity<ApiError> handleUnavailable(RuntimeException e) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(new ApiError("AI_UNAVAILABLE", "잠시 후 다시 시도해 주세요."));
  }
}
