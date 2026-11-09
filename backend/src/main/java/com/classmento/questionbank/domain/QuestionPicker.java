package com.classmento.questionbank.domain;

import java.util.List;
import java.util.Random;
import java.util.Set;

/** 면접에 낼 질문 고르기. */
public final class QuestionPicker {

  private QuestionPicker() {}

  public static List<Candidate> pick(
      List<Candidate> pool, Level level, Set<String> prefer, int count, Random random) {
    throw new UnsupportedOperationException("TODO");
  }

  /** keys: 이력서 기술과 맞춰 볼 소문자 이름들(태그·소분류). */
  public record Candidate(
      Long id, String text, String intent, Level level, Long subcategoryId, Set<String> keys) {}
}
