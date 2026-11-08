package com.classmento.llm;

import java.util.ArrayList;
import java.util.List;

/** 테스트용 LLM. 미리 넣어둔 응답을 돌려주고, 받은 요청을 기록한다. 실제 API를 호출하는 테스트는 만들지 않는다. */
public class FakeLlmClient implements LlmClient {

  private final String cannedText;
  private final List<LlmRequest> requests = new ArrayList<>();

  public FakeLlmClient(String cannedText) {
    this.cannedText = cannedText;
  }

  @Override
  public LlmResponse generate(LlmRequest request) {
    requests.add(request);
    return new LlmResponse(cannedText, "fake", 0, 0);
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  public List<LlmRequest> requests() {
    return requests;
  }
}
