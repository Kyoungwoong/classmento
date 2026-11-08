package com.classmento.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("classmento.llm")
public record LlmProperties(String apiKey, String model, long maxTokens) {

  public boolean configured() {
    return apiKey != null && !apiKey.isBlank();
  }
}
