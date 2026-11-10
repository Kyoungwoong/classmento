package com.classmento.interview.domain;

/** 질문 하나의 꼬리질문 상태(ADR 0008). "생성 중"은 저장하지 않고 계산한다: AWAITING인데 답변이 있으면 지금 판단하고 있는 것이다. */
public enum FollowUpState {
  /** 꼬리 대상이 아님(인성 질문, 꼬리 없음 옵션, 최대 깊이). */
  NONE,
  /** 대상이고 아직 정하지 않음. */
  AWAITING,
  /** 꼬리를 붙였음. */
  ASKED,
  /** 붙이지 않기로 함(AI가 충분하다고 봄, 짧은 답, 생성 실패). */
  CLOSED
}
