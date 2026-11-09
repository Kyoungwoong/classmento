package com.classmento.interview.infrastructure;

import com.classmento.interview.domain.InterviewSession;
import com.classmento.interview.domain.InterviewSessionRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/** 도메인 포트 {@link InterviewSessionRepository}의 Spring Data JPA 구현. */
public interface InterviewSessionJpaRepository
    extends JpaRepository<InterviewSession, Long>, InterviewSessionRepository {}
