package com.classmento.interview.domain;

import java.util.List;
import java.util.Optional;

/** 면접 세션 저장소 (도메인 포트). 구현: infrastructure.InterviewSessionJpaRepository. */
public interface InterviewSessionRepository {

  <S extends InterviewSession> S save(S session);

  Optional<InterviewSession> findById(Long id);

  Optional<InterviewSession> findByIdAndUserId(Long id, Long userId);

  List<InterviewSession> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
