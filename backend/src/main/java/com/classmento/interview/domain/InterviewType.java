package com.classmento.interview.domain;

/** 면접 탭 (ADR 0005). */
public enum InterviewType {
  /** 경험 기반: 이력서 경험 하나를 골라 관련 기술·CS 질문 */
  EXPERIENCE,
  /** 기술: 질문 은행 Language·Framework·CS·설계 */
  TECH,
  /** 인성: 질문 은행 인성 */
  BEHAVIORAL,
  /** 혼합: 기술 + 인성, 끝난 뒤 한꺼번에 평가 */
  MIXED
}
