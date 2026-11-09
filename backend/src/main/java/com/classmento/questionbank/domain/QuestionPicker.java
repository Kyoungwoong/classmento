package com.classmento.questionbank.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 면접에 낼 질문 고르기(순수 규칙).
 *
 * <ol>
 *   <li>연차가 있으면 같은 연차와 한 단계 아래만 후보(같은 연차가 먼저)
 *   <li>이력서 기술(prefer)과 맞는 질문이 먼저
 *   <li>같은 우선순위 안에서는 무작위
 *   <li>소분류가 겹치지 않게 먼저 고르고, 모자라면 남은 것으로 채운다
 * </ol>
 *
 * 후보가 모자라면 있는 만큼만 돌려준다(부족 판단은 면접 쪽이 한다).
 */
public final class QuestionPicker {

  private QuestionPicker() {}

  public static List<Candidate> pick(
      List<Candidate> pool, Level level, Set<String> prefer, int count, Random random) {
    List<Candidate> allowed = new ArrayList<>();
    for (Candidate c : pool) {
      if (level == null || distance(level, c.level()) >= 0) {
        allowed.add(c);
      }
    }
    Collections.shuffle(allowed, random);
    allowed.sort(
        Comparator.comparingInt((Candidate c) -> matches(c, prefer) ? 0 : 1)
            .thenComparingInt(c -> level == null ? 0 : distance(level, c.level())));

    List<Candidate> picked = new ArrayList<>();
    Set<Long> subs = new HashSet<>();
    for (Candidate c : allowed) {
      if (picked.size() < count && subs.add(c.subcategoryId())) {
        picked.add(c);
      }
    }
    for (Candidate c : allowed) {
      if (picked.size() < count && !picked.contains(c)) {
        picked.add(c);
      }
    }
    return picked;
  }

  /** 0 = 같은 연차, 1 = 한 단계 아래, -1 = 후보 아님 */
  private static int distance(Level wanted, Level actual) {
    int d = wanted.ordinal() - actual.ordinal();
    return d == 0 || d == 1 ? d : -1;
  }

  private static boolean matches(Candidate c, Set<String> prefer) {
    for (String k : c.keys()) {
      if (prefer.contains(k)) {
        return true;
      }
    }
    return false;
  }

  /** keys: 이력서 기술과 맞춰 볼 소문자 이름들(태그·소분류). */
  public record Candidate(
      Long id, String text, String intent, Level level, Long subcategoryId, Set<String> keys) {}
}
