package com.classmento.resume.domain;

import java.util.List;
import java.util.Optional;

/** 이력서 저장소 (도메인 포트). 구현: infrastructure.ResumeJpaRepository (Spring Data). */
public interface ResumeRepository {

  <S extends Resume> S save(S resume);

  Optional<Resume> findById(Long id);

  /** 본인 소유만 조회. 남의 이력서는 존재 여부도 드러내지 않도록 404로 처리한다. */
  Optional<Resume> findByIdAndUserId(Long id, Long userId);

  List<Resume> findByUserIdOrderByCreatedAtDesc(Long userId);
}
