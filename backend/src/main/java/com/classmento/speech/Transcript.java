package com.classmento.speech;

/**
 * @param text 인식된 전체 문장
 * @param durationMillis 음성 길이(알 수 없으면 0)
 */
public record Transcript(String text, long durationMillis) {}
