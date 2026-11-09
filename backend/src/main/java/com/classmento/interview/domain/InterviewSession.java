package com.classmento.interview.domain;

import java.util.List;

/** 면접 세션 (애그리거트 루트). */
public class InterviewSession {

  public static void requireValid(InterviewType type, int count, Conditions conditions) {
    throw new UnsupportedOperationException("TODO");
  }

  public static InterviewSession start(
      Long userId, InterviewType type, int count, Conditions conditions, List<QuestionDraft> q) {
    throw new UnsupportedOperationException("TODO");
  }

  public Long getUserId() {
    throw new UnsupportedOperationException("TODO");
  }

  public SessionStatus getStatus() {
    throw new UnsupportedOperationException("TODO");
  }

  public List<InterviewQuestion> getQuestions() {
    throw new UnsupportedOperationException("TODO");
  }
}
