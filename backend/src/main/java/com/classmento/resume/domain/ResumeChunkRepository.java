package com.classmento.resume.domain;

import java.util.List;

/**
 * 이력서 청크(검색 단위) + 임베딩 저장소 (도메인 포트). 벡터 kNN 검색을 제공한다. 구현: infrastructure.JdbcResumeChunkRepository
 * (pgvector).
 */
public interface ResumeChunkRepository {

  /** 해당 이력서의 기존 청크를 지우고 새로 저장한다(재처리 시 중복 방지). contents와 embeddings는 같은 순서·길이. */
  void replaceAll(Long resumeId, List<String> contents, List<float[]> embeddings);

  /** 질의 벡터와 가장 가까운 청크 k개(코사인 거리 오름차순). 질문 생성 RAG에서 사용. */
  List<ChunkMatch> findNearest(Long resumeId, float[] query, int k);

  int count(Long resumeId);

  /**
   * @param distance 코사인 거리(0 = 같은 방향, 2 = 반대)
   */
  record ChunkMatch(int seq, String content, double distance) {}
}
