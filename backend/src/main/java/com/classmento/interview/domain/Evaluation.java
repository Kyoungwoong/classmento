package com.classmento.interview.domain;

import com.classmento.common.domain.DomainException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 답변 평가 결과. 규칙(docs/ai/interview-and-rubric.md):
 *
 * <ul>
 *   <li>질문 종류의 루브릭 기준이 모두 있어야 한다(다른 기준은 버린다)
 *   <li>점수는 1~5
 *   <li>답변에서 인용한 근거가 없으면 3점을 넘을 수 없다(넘으면 3으로 낮춘다)
 *   <li>overall = 기준 평균(소수 첫째 자리 반올림)
 * </ul>
 */
public record Evaluation(
    Map<String, Score> scores,
    String strength,
    String improvement,
    String suggestion,
    double overall,
    String promptVersion,
    String model) {

  public static final int FEEDBACK_MAX = 500;

  public static Evaluation of(
      QuestionKind kind,
      Map<String, Score> raw,
      String strength,
      String improvement,
      String suggestion,
      String promptVersion,
      String model) {
    Map<String, Score> scores = new LinkedHashMap<>();
    int sum = 0;
    for (String criterion : Rubric.of(kind).criteria()) {
      Score s = raw == null ? null : raw.get(criterion);
      if (s == null || s.score() < 1 || s.score() > 5) {
        throw invalid();
      }
      boolean noEvidence = s.evidence() == null || s.evidence().isBlank();
      int score = noEvidence ? Math.min(s.score(), 3) : s.score();
      scores.put(criterion, new Score(score, noEvidence ? null : s.evidence().strip()));
      sum += score;
    }
    double overall = Math.round(sum * 10.0 / scores.size()) / 10.0;
    return new Evaluation(
        scores,
        feedback(strength),
        feedback(improvement),
        feedback(suggestion),
        overall,
        promptVersion,
        model);
  }

  private static String feedback(String s) {
    if (s == null || s.isBlank()) {
      throw invalid();
    }
    String t = s.strip();
    return t.length() > FEEDBACK_MAX ? t.substring(0, FEEDBACK_MAX) : t;
  }

  private static DomainException invalid() {
    return new DomainException("INVALID_EVALUATION", "평가 결과 형식이 맞지 않아요.");
  }
}
