package com.classmento.resume.infrastructure;

import com.classmento.resume.domain.Resume;
import com.classmento.resume.domain.ResumeRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/** 도메인 포트 {@link ResumeRepository}의 Spring Data JPA 구현. */
public interface ResumeJpaRepository extends JpaRepository<Resume, Long>, ResumeRepository {}
