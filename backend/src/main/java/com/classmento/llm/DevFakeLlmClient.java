package com.classmento.llm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;

/**
 * 로컬 개발용 가짜 LLM. {@code classpath:fake/<promptName>.json}의 고정 응답을 돌려준다. API 키 없이 화면까지 전체 흐름을 확인하기
 * 위한 것이며 prod에서는 등록되지 않는다(classmento.ai.fake=false).
 */
public class DevFakeLlmClient implements LlmClient {

  @Override
  public LlmResponse generate(LlmRequest request) {
    ClassPathResource resource = new ClassPathResource("fake/" + request.promptName() + ".json");
    if (!resource.exists()) {
      return new LlmResponse("{}", "dev-fake", 0, 0);
    }
    try (InputStream in = resource.getInputStream()) {
      return new LlmResponse(
          new String(in.readAllBytes(), StandardCharsets.UTF_8), "dev-fake", 0, 0);
    } catch (IOException e) {
      throw new LlmUnavailableException("가짜 응답 파일을 읽지 못했습니다.", e);
    }
  }

  @Override
  public boolean isAvailable() {
    return true;
  }
}
