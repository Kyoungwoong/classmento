package com.classmento.llm;

/**
 * @param promptName 사용한 프롬프트 파일 이름(예: "resume-parse.v1"). 기록·가짜 응답 선택에 쓴다.
 * @param systemPrompt {@link PromptLoader}로 읽은 시스템 프롬프트
 * @param userMessage 이력서·질문·답변 등 이번 호출의 입력
 */
public record LlmRequest(String promptName, String systemPrompt, String userMessage) {}
