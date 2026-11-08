package com.classmento.embedding;

import java.util.List;
import java.util.Locale;

/**
 * 로컬·테스트용 가짜 임베딩. "feature hashing" 방식: 단어마다 해시로 차원 하나를 골라 1을 더하고 길이를 1로 정규화한다. 의미는 모르지만 같은 단어를 많이
 * 공유하는 문장끼리 가깝게 나오므로 kNN 흐름을 키 없이 확인할 수 있다. 결정적(같은 입력 → 같은 벡터).
 */
public class HashEmbeddingClient implements EmbeddingClient {

  private final int dimensions;

  public HashEmbeddingClient(int dimensions) {
    this.dimensions = dimensions;
  }

  @Override
  public List<float[]> embedAll(List<String> texts) {
    return texts.stream().map(this::hash).toList();
  }

  private float[] hash(String text) {
    float[] v = new float[dimensions];
    for (String token : text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
      if (token.isBlank()) {
        continue;
      }
      v[Math.floorMod(token.hashCode(), dimensions)] += 1f;
    }
    double norm = 0;
    for (float x : v) {
      norm += x * x;
    }
    if (norm == 0) {
      v[0] = 1f; // 빈 문장도 0벡터가 되지 않게 (코사인 거리 계산 불가 방지)
      return v;
    }
    float inv = (float) (1 / Math.sqrt(norm));
    for (int i = 0; i < v.length; i++) {
      v[i] *= inv;
    }
    return v;
  }

  @Override
  public int dimensions() {
    return dimensions;
  }

  @Override
  public boolean isAvailable() {
    return true;
  }
}
