package com.classmento.questionbank.domain;

import java.util.List;
import java.util.Optional;

/** 질문 저장소 (도메인 포트). 구현: infrastructure.QuestionJpaRepository. */
public interface QuestionRepository {

  <S extends Question> S save(S question);

  Optional<Question> findById(Long id);

  List<Question> findAllById(Iterable<Long> ids);

  /** 동시 수정 방지용 비관적 잠금 조회(트랜잭션 안에서만). */
  Optional<Question> lockById(Long id);
}
