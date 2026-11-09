package com.classmento.interview.domain;

import java.util.List;
import java.util.Optional;

/** 면접 세션 저장소 (도메인 포트). 구현: infrastructure.InterviewSessionJpaRepository. */
public interface InterviewSessionRepository {

  <S extends InterviewSession> S save(S session);

  Optional<InterviewSession> findById(Long id);

  Optional<InterviewSession> findByIdAndUserId(Long id, Long userId);

  /** 끝내기·종합처럼 상태를 바꿀 때 잠그고 읽는다(트랜잭션 안에서만). */
  Optional<InterviewSession> lockById(Long id);

  List<InterviewSession> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
