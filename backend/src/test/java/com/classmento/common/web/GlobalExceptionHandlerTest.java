package com.classmento.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  /** 서비스 검사와 insert 사이 경쟁 등으로 DB 제약(유니크 등)에 걸리면 500이 아니라 409로 다시 시도를 안내한다. */
  @Test
  void DB_제약_위반은_409_CONFLICT() {
    ResponseEntity<ApiError> res =
        new GlobalExceptionHandler()
            .handleDataIntegrity(
                new DataIntegrityViolationException("uq_qb_category_sibling_name"));

    assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(res.getBody().code()).isEqualTo("CONFLICT");
    assertThat(res.getBody().message()).isEqualTo("이미 있는 값과 겹쳐요. 확인 후 다시 시도해 주세요.");
  }
}
