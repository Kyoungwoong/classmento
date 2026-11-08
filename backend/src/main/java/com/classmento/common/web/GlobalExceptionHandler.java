package com.classmento.common.web;

import com.classmento.common.domain.DomainException;
import com.classmento.embedding.EmbeddingUnavailableException;
import com.classmento.llm.LlmUnavailableException;
import com.classmento.speech.SttUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiError> handleApi(ApiException e) {
    return ResponseEntity.status(e.status()).body(new ApiError(e.code(), e.getMessage()));
  }

  /** 도메인 불변식 위반(예: 아직 파싱 중인 이력서 수정) → 409 */
  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ApiError> handleDomain(DomainException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(e.code(), e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
    String message =
        e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(err -> err.getDefaultMessage())
            .orElse("입력한 내용을 다시 확인해 주세요.");
    return ResponseEntity.badRequest().body(new ApiError("INVALID_REQUEST", message));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new ApiError("LOGIN_FAILED", "이메일 또는 비밀번호가 맞지 않아요."));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ApiError> handleTooLarge(MaxUploadSizeExceededException e) {
    return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
        .body(new ApiError("FILE_TOO_LARGE", "파일이 너무 커요. 20MB 이하로 올려주세요."));
  }

  @ExceptionHandler({
    LlmUnavailableException.class,
    SttUnavailableException.class,
    EmbeddingUnavailableException.class
  })
  public ResponseEntity<ApiError> handleUnavailable(RuntimeException e) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(new ApiError("AI_UNAVAILABLE", "잠시 후 다시 시도해 주세요."));
  }
}
