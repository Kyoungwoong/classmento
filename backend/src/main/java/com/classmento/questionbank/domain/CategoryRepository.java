package com.classmento.questionbank.domain;

import java.util.List;
import java.util.Optional;

/** 카테고리 저장소 (도메인 포트). 구현: infrastructure.CategoryJpaRepository. */
public interface CategoryRepository {

  <S extends Category> S save(S category);

  Optional<Category> findById(Long id);

  List<Category> findAll();
}
