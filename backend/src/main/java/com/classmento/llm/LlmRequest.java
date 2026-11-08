package com.classmento.llm;

/**
 * @param systemPrompt {@link PromptLoader}로 읽은 시스템 프롬프트
 * @param userMessage 이력서·질문·답변 등 이번 호출의 입력
 */
public record LlmRequest(String systemPrompt, String userMessage) {}
