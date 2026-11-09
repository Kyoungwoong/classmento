package com.classmento.questionbank.domain;

import java.util.List;

/** 질문 임베딩 저장·검색 (도메인 포트). 구현: infrastructure.JdbcQuestionEmbeddingRepository (pgvector). */
public interface QuestionEmbeddingRepository {

  void update(Long questionId, float[] embedding);

  /** 임베딩이 비어 있는 질문을 최대 limit개(임베딩 문장 재료와 함께). */
  List<Missing> findMissing(int limit);

  /** 보이는 질문 중 질의 벡터와 가장 가까운 k개. */
  List<Match> findNearest(float[] query, int k);

  record Missing(
      Long questionId, String topName, String subName, String text, List<String> tagNames) {

    public String embeddingText() {
      return QuestionEmbeddingText.of(topName, subName, text, tagNames);
    }
  }

  record Match(Long questionId, String text, double distance) {}
}
