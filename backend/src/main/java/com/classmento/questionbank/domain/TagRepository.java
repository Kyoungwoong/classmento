package com.classmento.questionbank.domain;

import java.util.List;
import java.util.Optional;

/** 태그 저장소 (도메인 포트). 구현: infrastructure.TagJpaRepository. */
public interface TagRepository {

  <S extends Tag> S save(S tag);

  Optional<Tag> findByKey(String key);

  List<Tag> findAllById(Iterable<Long> ids);

  /**
   * 자동완성. 키에 검색어가 들어간 태그를, 앞부분이 일치하는 것부터 최대 limit개.
   *
   * @param likeEscapedKey LIKE 특수문자(\, %, _)를 \로 이스케이프한 소문자 키
   */
  List<Tag> search(String likeEscapedKey, int limit);
}
