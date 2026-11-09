package com.classmento.interview.infrastructure;

import com.classmento.interview.domain.InterviewSession;
import com.classmento.interview.domain.InterviewSessionRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 도메인 포트 {@link InterviewSessionRepository}의 Spring Data JPA 구현. */
public interface InterviewSessionJpaRepository
    extends JpaRepository<InterviewSession, Long>, InterviewSessionRepository {

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT s FROM InterviewSession s WHERE s.id = :id")
  Optional<InterviewSession> lockById(@Param("id") Long id);
}
