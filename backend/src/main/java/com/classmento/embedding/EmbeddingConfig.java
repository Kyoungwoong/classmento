package com.classmento.embedding;

import com.classmento.common.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfig {

  private static final Logger log = LoggerFactory.getLogger(EmbeddingConfig.class);

  /** 우선순위: 실제 키 → (로컬) 해시 임베딩 → 사용 불가(503). */
  @Bean
  EmbeddingClient embeddingClient(EmbeddingProperties properties, AiProperties ai) {
    if (properties.configured()) {
      return new OpenAiEmbeddingClient(properties);
    }
    if (ai.fake()) {
      log.warn("OPENAI_API_KEY 없음 → 해시 임베딩(HashEmbeddingClient) 사용 중. 검색 품질은 실제와 다릅니다.");
      return new HashEmbeddingClient(properties.dimensions());
    }
    return new UnavailableEmbeddingClient(properties.dimensions());
  }
}
