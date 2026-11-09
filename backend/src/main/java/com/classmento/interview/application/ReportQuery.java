package com.classmento.interview.application;

import com.classmento.common.web.ApiException;
import com.classmento.interview.domain.Answer;
import com.classmento.interview.domain.AnswerRepository;
import com.classmento.interview.domain.Evaluation;
import com.classmento.interview.domain.InterviewQuestion;
import com.classmento.interview.domain.InterviewSession;
import com.classmento.interview.domain.InterviewSessionRepository;
import com.classmento.interview.domain.Rubric;
import com.classmento.interview.domain.Score;
import com.classmento.interview.domain.SessionReport;
import com.classmento.interview.domain.SessionStatus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 면접 리포트 조회: 종합(끝났을 때) + 기준별 평균 + 문항별(내 답변 글자·점수·피드백). 본인 것만.
 *
 * <p>부수효과: 끝났고 모두 평가됐는데 종합이 없으면 종합을 다시 요청한다({@link ReportScheduler}, 세션마다 하나만).
 */
@Service
public class ReportQuery {

  private final InterviewSessionRepository sessions;
  private final AnswerRepository answers;
  private final ReportScheduler reports;

  public ReportQuery(
      InterviewSessionRepository sessions, AnswerRepository answers, ReportScheduler reports) {
    this.sessions = sessions;
    this.answers = answers;
    this.reports = reports;
  }

  public Report report(Long userId, Long sessionId) {
    InterviewSession s =
        sessions.findByIdAndUserId(sessionId, userId).orElseThrow(ApiException::notFound);
    Map<Integer, Answer> bySeq = new HashMap<>();
    answers.findBySessionIdOrderBySeq(sessionId).forEach(a -> bySeq.put(a.getSeq(), a));

    List<Item> items = new ArrayList<>();
    Map<String, int[]> sums = new HashMap<>(); // criterion → [합, 개수]
    for (InterviewQuestion q : s.getQuestions()) {
      Answer a = bySeq.get(q.getSeq());
      Evaluation e = a == null ? null : a.getEvaluation();
      if (e != null) {
        e.scores()
            .forEach(
                (k, v) -> {
                  int[] acc = sums.computeIfAbsent(k, x -> new int[2]);
                  acc[0] += v.score();
                  acc[1]++;
                });
      }
      items.add(new Item(q, a, e));
    }

    List<CriterionAverage> criteria = new ArrayList<>();
    for (Rubric r : Rubric.values()) {
      for (String c : r.criteria()) {
        int[] acc = sums.get(c);
        if (acc != null) {
          criteria.add(new CriterionAverage(c, ReportWriter.round((double) acc[0] / acc[1])));
        }
      }
    }
    SessionReport report = s.getReport();
    boolean ready = s.getStatus() == SessionStatus.REPORTED;
    boolean allEvaluated = items.stream().allMatch(it -> it.evaluation() != null);
    if (s.getStatus() == SessionStatus.FINISHED && allEvaluated) {
      // 끝내기 직후 종합 작업이 거절·실패했으면 여기서 다시 시도한다(잠금·상태 확인으로 한 번만 저장)
      reports.request(sessionId);
    }
    return new Report(s, ready, report, criteria, items);
  }

  public record CriterionAverage(String criterion, double average) {}

  /** 문항 하나: 질문, 답변(없을 수 있음), 평가(끝났을 때). */
  public record Item(InterviewQuestion question, Answer answer, Evaluation evaluation) {

    public Map<String, Score> scores() {
      return evaluation == null ? Map.of() : evaluation.scores();
    }
  }

  public record Report(
      InterviewSession session,
      boolean ready,
      SessionReport summary,
      List<CriterionAverage> criteria,
      List<Item> items) {}
}
