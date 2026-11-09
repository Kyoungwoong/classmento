package com.classmento.interview.domain;

import java.util.List;

/** 평가 루브릭(docs/ai/interview-and-rubric.md). 기준 이름은 프롬프트 출력 JSON의 키와 같다. */
public enum Rubric {
  TECH(List.of("accuracy", "depth", "problem_solving", "communication")),
  BEHAVIORAL(List.of("specificity", "ownership", "collaboration", "reflection"));

  private final List<String> criteria;

  Rubric(List<String> criteria) {
    this.criteria = criteria;
  }

  public List<String> criteria() {
    return criteria;
  }

  public static Rubric of(QuestionKind kind) {
    return kind == QuestionKind.BEHAVIORAL ? BEHAVIORAL : TECH;
  }
}
