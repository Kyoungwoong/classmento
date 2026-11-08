package com.classmento.llm;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LlmConfig {

  @Bean
  LlmClient llmClient(LlmProperties properties) {
    return properties.configured() ? new ClaudeLlmClient(properties) : new UnavailableLlmClient();
  }
}
