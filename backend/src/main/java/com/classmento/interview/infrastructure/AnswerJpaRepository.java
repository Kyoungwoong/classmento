package com.classmento.interview.infrastructure;

import com.classmento.interview.domain.Answer;
import com.classmento.interview.domain.AnswerRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 도메인 포트 {@link AnswerRepository}의 Spring Data JPA 구현. */
public interface AnswerJpaRepository extends JpaRepository<Answer, Long>, AnswerRepository {

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM Answer a WHERE a.sessionId = :sessionId AND a.seq = :seq")
  Optional<Answer> lockBySessionIdAndSeq(@Param("sessionId") Long sessionId, @Param("seq") int seq);

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM Answer a WHERE a.id = :id")
  Optional<Answer> lockById(@Param("id") Long id);
}
