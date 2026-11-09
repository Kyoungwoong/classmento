package com.classmento.questionbank.infrastructure;

import com.classmento.questionbank.domain.Category;
import com.classmento.questionbank.domain.CategoryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

/** 도메인 포트 {@link CategoryRepository}의 Spring Data JPA 구현. */
public interface CategoryJpaRepository extends JpaRepository<Category, Long>, CategoryRepository {}
