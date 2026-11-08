package com.classmento.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param fake true면 API 키가 없는 AI 연동(LLM, 임베딩)을 가짜 구현으로 대체한다. 로컬 개발·테스트 전용.
 */
@ConfigurationProperties("classmento.ai")
public record AiProperties(boolean fake) {}
