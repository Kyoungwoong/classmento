package com.classmento.llm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * {@code classpath:prompts/<name>.md}를 읽는다. 프롬프트는 코드에 문자열로 박지 않고 반드시 이 경로의 파일로 관리한다. 규칙:
 * docs/ai/interview-and-rubric.md
 */
@Component
public class PromptLoader {

  private final Map<String, String> cache = new ConcurrentHashMap<>();

  public String load(String name) {
    return cache.computeIfAbsent(name, PromptLoader::read);
  }

  private static String read(String name) {
    ClassPathResource resource = new ClassPathResource("prompts/" + name + ".md");
    try (InputStream in = resource.getInputStream()) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalArgumentException("프롬프트 파일을 찾을 수 없습니다: " + name, e);
    }
  }
}
