package com.classmento.questionbank.infrastructure;

import com.classmento.questionbank.domain.Question;
import com.classmento.questionbank.domain.QuestionRepository;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 도메인 포트 {@link QuestionRepository}의 Spring Data JPA 구현. */
public interface QuestionJpaRepository extends JpaRepository<Question, Long>, QuestionRepository {

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT q FROM Question q WHERE q.id = :id")
  Optional<Question> lockById(@Param("id") Long id);
}
