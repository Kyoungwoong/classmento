package com.classmento.llm;

/**
 * LLM 포트. 도메인 코드(interview, evaluation)는 이 인터페이스만 의존한다. 구현체: {@link ClaudeLlmClient}, 키가 없을 때
 * {@link UnavailableLlmClient}, 테스트에서는 FakeLlmClient.
 */
public interface LlmClient {

  LlmResponse generate(LlmRequest request);

  boolean isAvailable();
}
