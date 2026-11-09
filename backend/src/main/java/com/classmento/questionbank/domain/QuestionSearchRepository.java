package com.classmento.questionbank.domain;

import java.util.List;
import java.util.Map;

/** 관리 화면용 질문 검색 (도메인 포트). 구현: infrastructure.JdbcQuestionSearchRepository. */
public interface QuestionSearchRepository {

  /** 조건에 맞는 질문 ID를 최신순으로 한 페이지. */
  IdPage search(Filter filter, int page, int size);

  /** 소분류 ID → 질문 수(숨긴 질문 포함). */
  Map<Long, Long> countByCategory();

  /** null인 조건은 거르지 않는다. */
  record Filter(
      Long topCategoryId,
      Long categoryId,
      Level level,
      JobRole jobRole,
      Long tagId,
      String query,
      boolean includeHidden) {}

  record IdPage(List<Long> ids, long total) {}
}
