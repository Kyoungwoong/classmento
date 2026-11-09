package com.classmento.interview.domain;

import java.util.Map;

/** 답변 평가 결과. */
public record Evaluation(
    Map<String, Score> scores,
    String strength,
    String improvement,
    String suggestion,
    double overall,
    String promptVersion,
    String model) {

  public static Evaluation of(
      QuestionKind kind,
      Map<String, Score> raw,
      String strength,
      String improvement,
      String suggestion,
      String promptVersion,
      String model) {
    throw new UnsupportedOperationException("TODO");
  }
}
