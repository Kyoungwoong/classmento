package com.classmento.interview.domain;

public enum AnswerStatus {
  /** 녹음을 받음(STT 대기) */
  UPLOADED,
  /** 글자로 바꿈(평가 대기 — 혼합은 끝낸 뒤 평가) */
  TRANSCRIBED,
  /** 평가까지 끝남 */
  EVALUATED,
  /** STT나 평가 실패(다시 시도 가능) */
  FAILED
}
