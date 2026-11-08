package com.classmento.resume.infrastructure;

import com.classmento.resume.domain.Resume;
import com.classmento.resume.domain.ResumeRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 도메인 포트 {@link ResumeRepository}의 Spring Data JPA 구현. */
public interface ResumeJpaRepository extends JpaRepository<Resume, Long>, ResumeRepository {

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Resume r where r.id = :id and r.userId = :userId")
  Optional<Resume> lockByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
