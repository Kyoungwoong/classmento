package com.classmento.llm;

/** ANTHROPIC_API_KEY가 없을 때 등록된다. 앱은 뜨지만 AI 기능 호출 시 503(AI_UNAVAILABLE)을 반환한다. */
public class UnavailableLlmClient implements LlmClient {

  @Override
  public LlmResponse generate(LlmRequest request) {
    throw new LlmUnavailableException("ANTHROPIC_API_KEY가 설정되지 않았습니다.");
  }

  @Override
  public boolean isAvailable() {
    return false;
  }
}
