package com.classmento.interview.domain;

import java.util.List;
import java.util.Optional;

/** 답변 저장소 (도메인 포트). 구현: infrastructure.AnswerJpaRepository. */
public interface AnswerRepository {

  <S extends Answer> S save(S answer);

  Optional<Answer> findById(Long id);

  Optional<Answer> findBySessionIdAndSeq(Long sessionId, int seq);

  List<Answer> findBySessionIdOrderBySeq(Long sessionId);

  /** 같은 답변을 동시에 고치지 않게 잠그고 읽는다(트랜잭션 안에서만). */
  Optional<Answer> lockBySessionIdAndSeq(Long sessionId, int seq);

  /** 백그라운드 처리 결과를 저장할 때(트랜잭션 안에서만). */
  Optional<Answer> lockById(Long id);
}
