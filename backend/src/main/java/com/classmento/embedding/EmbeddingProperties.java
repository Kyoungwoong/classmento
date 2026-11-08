package com.classmento.embedding;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("classmento.embedding")
public record EmbeddingProperties(String apiKey, String model, int dimensions) {

  public boolean configured() {
    return apiKey != null && !apiKey.isBlank();
  }
}
