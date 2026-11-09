package com.classmento.interview.application;

import com.classmento.interview.domain.Answer;
import com.classmento.interview.domain.AnswerRepository;
import com.classmento.interview.domain.AnswerStatus;
import com.classmento.interview.domain.Evaluation;
import com.classmento.interview.domain.InterviewSession;
import com.classmento.interview.domain.InterviewSessionRepository;
import com.classmento.interview.domain.SessionReport;
import com.classmento.interview.domain.SessionStatus;
import com.classmento.llm.LlmClient;
import com.classmento.llm.LlmRequest;
import com.classmento.llm.LlmResponse;
import com.classmento.llm.PromptLoader;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * 끝난 면접의 모든 답변 평가가 끝나면 종합(session-report 프롬프트)을 한 번 만든다. 답변 평가가 끝날 때마다, 그리고 끝내기 직후에 불린다.
 *
 * <p>종합 문장을 못 만들면(AI 오류) 가장 높은 답변의 잘한 점·가장 낮은 답변의 고칠 점으로 대신한다. 점수는 답변 종합 점수의 평균.
 */
@Component
public class ReportWriter {

  static final String PROMPT = "session-report.v1";
  private static final Logger log = LoggerFactory.getLogger(ReportWriter.class);

  private final InterviewSessionRepository sessions;
  private final AnswerRepository answers;
  private final LlmClient llm;
  private final PromptLoader prompts;
  private final ObjectMapper json;
  private final TransactionTemplate tx;

  public ReportWriter(
      InterviewSessionRepository sessions,
      AnswerRepository answers,
      LlmClient llm,
      PromptLoader prompts,
      ObjectMapper json,
      TransactionTemplate tx) {
    this.sessions = sessions;
    this.answers = answers;
    this.llm = llm;
    this.prompts = prompts;
    this.json = json;
    this.tx = tx;
  }

  public void maybeReport(Long sessionId) {
    InterviewSession session = sessions.findById(sessionId).orElse(null);
    if (session == null || session.getStatus() != SessionStatus.FINISHED) {
      return;
    }
    List<Answer> list = answers.findBySessionIdOrderBySeq(sessionId);
    if (list.isEmpty() || list.stream().anyMatch(a -> a.getStatus() != AnswerStatus.EVALUATED)) {
      return;
    }
    double overall =
        round(list.stream().mapToDouble(a -> a.getEvaluation().overall()).average().orElse(0));
    SessionReport report = summarize(session, list, overall);
    tx.executeWithoutResult(
        s ->
            sessions
                .lockById(sessionId)
                .filter(locked -> locked.getStatus() == SessionStatus.FINISHED)
                .ifPresent(locked -> locked.report(report)));
  }

  private SessionReport summarize(InterviewSession session, List<Answer> list, double overall) {
    try {
      LlmResponse res =
          llm.generate(new LlmRequest(PROMPT, prompts.load(PROMPT), items(session, list)));
      Output out = json.readValue(stripFence(res.text()), Output.class);
      if (out.strength() != null
          && !out.strength().isBlank()
          && out.improvement() != null
          && !out.improvement().isBlank()) {
        return new SessionReport(
            overall, cut(out.strength()), cut(out.improvement()), PROMPT, res.model());
      }
    } catch (RuntimeException e) {
      log.warn(
          "면접 {} 종합 문장 만들기 실패, 답변 피드백으로 대신: {}", session.getId(), e.getClass().getSimpleName());
    }
    Evaluation best =
        list.stream()
            .map(Answer::getEvaluation)
            .max(Comparator.comparingDouble(Evaluation::overall))
            .orElseThrow();
    Evaluation worst =
        list.stream()
            .map(Answer::getEvaluation)
            .min(Comparator.comparingDouble(Evaluation::overall))
            .orElseThrow();
    return new SessionReport(overall, best.strength(), worst.improvement(), "fallback", null);
  }

  private static String items(InterviewSession session, List<Answer> list) {
    StringBuilder sb = new StringBuilder("<items>\n");
    for (Answer a : list) {
      Evaluation e = a.getEvaluation();
      sb.append("- 질문: ").append(session.question(a.getSeq()).getText()).append('\n');
      sb.append("  종합: ").append(e.overall()).append(", 기준: ");
      e.scores().forEach((k, v) -> sb.append(k).append('=').append(v.score()).append(' '));
      sb.append("\n  잘한 점: ").append(e.strength()).append('\n');
      sb.append("  고칠 점: ").append(e.improvement()).append('\n');
    }
    return sb.append("</items>").toString();
  }

  static double round(double v) {
    return Math.round(v * 10) / 10.0;
  }

  private static String cut(String s) {
    String t = s.strip();
    return t.length() > Evaluation.FEEDBACK_MAX ? t.substring(0, Evaluation.FEEDBACK_MAX) : t;
  }

  private static String stripFence(String s) {
    String t = s.strip();
    if (t.startsWith("```")) {
      int firstNewline = t.indexOf('\n');
      int lastFence = t.lastIndexOf("```");
      if (firstNewline > 0 && lastFence > firstNewline) {
        return t.substring(firstNewline + 1, lastFence).strip();
      }
    }
    return t;
  }

  private record Output(String strength, String improvement) {}
}
