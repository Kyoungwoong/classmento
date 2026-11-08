package com.classmento.resume.application;

import com.classmento.llm.LlmClient;
import com.classmento.llm.LlmRequest;
import com.classmento.llm.LlmResponse;
import com.classmento.llm.PromptLoader;
import com.classmento.resume.domain.ParsedResume;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** 추출 텍스트 → Claude(resume-parse 프롬프트) → {@link ParsedResume}. JSON이 깨지면 1회 재시도. */
@Component
public class ResumeParser {

  static final String PROMPT = "resume-parse.v2";
  private static final int MAX_INPUT_CHARS = 30_000;

  private final LlmClient llm;
  private final PromptLoader prompts;
  private final ObjectMapper json;

  public ResumeParser(LlmClient llm, PromptLoader prompts, ObjectMapper json) {
    this.llm = llm;
    this.prompts = prompts;
    this.json = json;
  }

  public Result parse(String extractedText) {
    String text =
        extractedText.length() > MAX_INPUT_CHARS
            ? extractedText.substring(0, MAX_INPUT_CHARS)
            : extractedText;
    LlmRequest request =
        new LlmRequest(PROMPT, prompts.load(PROMPT), "<resume>\n" + text + "\n</resume>");

    RuntimeException last = null;
    for (int attempt = 0; attempt < 2; attempt++) {
      LlmResponse response = llm.generate(request);
      try {
        ParsedResume parsed = json.readValue(stripFence(response.text()), ParsedResume.class);
        return new Result(parsed, json.writeValueAsString(parsed), response.model(), PROMPT);
      } catch (JacksonException e) {
        last = e;
      }
    }
    throw new IllegalStateException("이력서 구조화 결과(JSON)를 해석하지 못했습니다.", last);
  }

  /** 모델이 ```json ... ``` 으로 감싸 답한 경우 벗긴다. */
  static String stripFence(String s) {
    String t = s.strip();
    if (t.startsWith("```")) {
      int firstNewline = t.indexOf('\n');
      int lastFence = t.lastIndexOf("```");
      if (firstNewline > 0 && lastFence > firstNewline) {
        return t.substring(firstNewline + 1, lastFence).strip();
      }
    }
    return t;
  }

  public record Result(ParsedResume parsed, String json, String model, String promptVersion) {}
}
