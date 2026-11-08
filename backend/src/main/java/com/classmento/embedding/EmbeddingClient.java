package com.classmento.embedding;

import java.util.List;

/**
 * 텍스트 → 벡터 포트. 벡터는 pgvector 컬럼(vector(1536))에 저장되고 kNN 검색에 쓰인다. 구현체: {@link OpenAiEmbeddingClient},
 * 로컬 가짜 {@link HashEmbeddingClient}, 키 없을 때 {@link UnavailableEmbeddingClient}.
 */
public interface EmbeddingClient {

  /** 입력 순서대로 벡터를 돌려준다. 모든 벡터의 길이는 {@link #dimensions()}. */
  List<float[]> embedAll(List<String> texts);

  default float[] embed(String text) {
    return embedAll(List.of(text)).get(0);
  }

  int dimensions();

  boolean isAvailable();
}
