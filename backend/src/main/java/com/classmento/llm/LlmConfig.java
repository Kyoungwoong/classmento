package com.classmento.llm;

import com.classmento.common.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfig {

  private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

  /** 우선순위: 실제 키 → (로컬) 가짜 → 사용 불가(503). */
  @Bean
  LlmClient llmClient(LlmProperties properties, AiProperties ai) {
    if (properties.configured()) {
      return new ClaudeLlmClient(properties);
    }
    if (ai.fake()) {
      log.warn("ANTHROPIC_API_KEY 없음 → 가짜 LLM(DevFakeLlmClient) 사용 중. 실제 결과가 아닙니다.");
      return new DevFakeLlmClient();
    }
    return new UnavailableLlmClient();
  }
}
