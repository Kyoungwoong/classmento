package com.classmento.llm;

public record LlmResponse(String text, String model, long inputTokens, long outputTokens) {}
