package com.classmento.resume;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeRepository extends JpaRepository<Resume, Long> {

  /** 본인 소유만 조회. 남의 이력서는 존재 여부도 드러내지 않도록 404로 처리한다. */
  Optional<Resume> findByIdAndUserId(Long id, Long userId);

  List<Resume> findByUserIdOrderByCreatedAtDesc(Long userId);
}
