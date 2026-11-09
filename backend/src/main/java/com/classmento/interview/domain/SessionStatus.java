package com.classmento.interview.domain;

public enum SessionStatus {
  /** 답하는 중 */
  IN_PROGRESS,
  /** 다 답하고 끝냄(평가 중일 수 있음) */
  FINISHED,
  /** 종합 리포트까지 나옴 */
  REPORTED
}
