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
import org.springframework.transaction.annotation.Transactional;

/** 면접 리포트 조회: 종합(끝났을 때) + 기준별 평균 + 문항별(내 답변 글자·점수·피드백). 본인 것만. */
@Service
public class ReportQuery {

  private final InterviewSessionRepository sessions;
  private final AnswerRepository answers;

  public ReportQuery(InterviewSessionRepository sessions, AnswerRepository answers) {
    this.sessions = sessions;
    this.answers = answers;
  }

  @Transactional(readOnly = true)
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
