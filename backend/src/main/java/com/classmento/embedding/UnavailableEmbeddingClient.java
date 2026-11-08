package com.classmento.embedding;

import java.util.List;

/** OPENAI_API_KEY가 없고 가짜 모드도 꺼져 있을 때 등록된다. */
public class UnavailableEmbeddingClient implements EmbeddingClient {

  private final int dimensions;

  public UnavailableEmbeddingClient(int dimensions) {
    this.dimensions = dimensions;
  }

  @Override
  public List<float[]> embedAll(List<String> texts) {
    throw new EmbeddingUnavailableException("OPENAI_API_KEY가 설정되지 않았습니다.");
  }

  @Override
  public int dimensions() {
    return dimensions;
  }

  @Override
  public boolean isAvailable() {
    return false;
  }
}
