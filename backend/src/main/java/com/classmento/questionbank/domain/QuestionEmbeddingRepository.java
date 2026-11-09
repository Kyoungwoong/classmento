package com.classmento.questionbank.domain;

import java.util.List;

/** 질문 임베딩 저장·검색 (도메인 포트). 구현: infrastructure.JdbcQuestionEmbeddingRepository (pgvector). */
public interface QuestionEmbeddingRepository {

  void update(Long questionId, float[] embedding);

  /** 보이는 질문 중 질의 벡터와 가장 가까운 k개. */
  List<Match> findNearest(float[] query, int k);

  record Match(Long questionId, String text, double distance) {}
}
