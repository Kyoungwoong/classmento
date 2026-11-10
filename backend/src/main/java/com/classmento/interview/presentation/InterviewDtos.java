package com.classmento.interview.presentation;

import com.classmento.interview.application.InterviewService.Command;
import com.classmento.interview.application.ReportQuery;
import com.classmento.interview.domain.Answer;
import com.classmento.interview.domain.AnswerStatus;
import com.classmento.interview.domain.Evaluation;
import com.classmento.interview.domain.FollowUpState;
import com.classmento.interview.domain.InterviewLevel;
import com.classmento.interview.domain.InterviewQuestion;
import com.classmento.interview.domain.InterviewSession;
import com.classmento.interview.domain.InterviewType;
import com.classmento.interview.domain.JobRole;
import com.classmento.interview.domain.QuestionKind;
import com.classmento.interview.domain.SessionReport;
import com.classmento.interview.domain.SessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 면접 API 요청/응답. 질문 의도(intent)는 면접 중에는 보여주지 않는다(리포트에서). */
public final class InterviewDtos {

  private InterviewDtos() {}

  public record CreateInterviewRequest(
      @NotNull(message = "면접 종류를 골라 주세요.") InterviewType type,
      @NotNull(message = "질문 수를 골라 주세요.") Integer count,
      InterviewLevel level,
      JobRole jobRole,
      Long resumeId,
      Long experienceId,
      Boolean narrowByResume,
      @Schema(description = "질문 하나에 붙일 꼬리질문 최대 수(0~2, 없으면 0). 인성 면접은 0만") Integer followUps) {

    Command toCommand() {
      return new Command(
          type,
          count,
          level,
          jobRole,
          resumeId,
          experienceId,
          Boolean.TRUE.equals(narrowByResume),
          followUps == null ? 0 : followUps);
    }
  }

  /**
   * answerStatus: 답하지 않았으면 없음. failureReason: 실패했을 때 원인과 할 일. 꼬리질문(ADR 0008): parentSeq(꼬리면 바로 앞
   * 질문), depth(0 = 본 질문), followUp(AWAITING이면서 답이 있으면 다음 질문을 고르는 중). retryable: 글자는 있고 평가만 실패해 다시
   * 녹음하지 않고 다시 평가하면 되는 실패.
   */
  @Schema(requiredProperties = {"seq", "text", "kind", "depth", "followUp", "retryable"})
  public record InterviewQuestionView(
      int seq,
      String text,
      QuestionKind kind,
      AnswerStatus answerStatus,
      String failureReason,
      Integer parentSeq,
      int depth,
      FollowUpState followUp,
      boolean retryable) {

    static InterviewQuestionView of(InterviewQuestion q, Answer a) {
      boolean retryable =
          a != null
              && a.getStatus() == AnswerStatus.FAILED
              && a.getTranscript() != null
              && !a.getTranscript().isBlank();
      return new InterviewQuestionView(
          q.getSeq(),
          q.getText(),
          q.getKind(),
          a == null ? null : a.getStatus(),
          a == null ? null : a.getFailureReason(),
          q.getParentSeq(),
          q.getDepth(),
          q.getFollowUp(),
          retryable);
    }
  }

  @Schema(requiredProperties = {"seq", "status"})
  public record AnswerStatusView(int seq, AnswerStatus status) {

    static AnswerStatusView of(Answer a) {
      return new AnswerStatusView(a.getSeq(), a.getStatus());
    }
  }

  /** questions는 화면 순서(본 질문 → 그 꼬리들 → 다음 본 질문). maxFollowUps: 질문 하나에 붙는 꼬리질문 최대 수. */
  @Schema(requiredProperties = {"id", "type", "status", "maxFollowUps", "questions", "createdAt"})
  public record InterviewView(
      Long id,
      InterviewType type,
      SessionStatus status,
      InterviewLevel level,
      JobRole jobRole,
      Long resumeId,
      Long experienceId,
      int maxFollowUps,
      List<InterviewQuestionView> questions,
      Instant createdAt) {

    static InterviewView of(InterviewSession s) {
      return of(s, List.of());
    }

    static InterviewView of(InterviewSession s, List<Answer> answers) {
      Map<Integer, Answer> bySeq = new HashMap<>();
      answers.forEach(a -> bySeq.put(a.getSeq(), a));
      return new InterviewView(
          s.getId(),
          s.getType(),
          s.getStatus(),
          s.getLevel(),
          s.getJobRole(),
          s.getResumeId(),
          s.getExperienceId(),
          s.getMaxFollowUps(),
          s.getQuestions().stream()
              .map(q -> InterviewQuestionView.of(q, bySeq.get(q.getSeq())))
              .toList(),
          s.getCreatedAt());
    }
  }

  @Schema(requiredProperties = {"id", "type", "status", "questionCount", "createdAt"})
  public record InterviewSummary(
      Long id, InterviewType type, SessionStatus status, int questionCount, Instant createdAt) {

    static InterviewSummary of(InterviewSession s) {
      return new InterviewSummary(
          s.getId(), s.getType(), s.getStatus(), s.rootCount(), s.getCreatedAt());
    }
  }

  @Schema(requiredProperties = {"criterion", "score"})
  public record ScoreView(String criterion, int score, String evidence) {}

  @Schema(requiredProperties = {"criterion", "average"})
  public record CriterionView(String criterion, double average) {}

  /** 문항 하나. 답하지 않았으면 answerStatus부터 비어 있고, 평가 전이면 점수·피드백이 비어 있다. */
  @Schema(requiredProperties = {"seq", "question", "kind", "depth", "scores"})
  public record ReportItemView(
      int seq,
      Integer parentSeq,
      int depth,
      String question,
      String intent,
      QuestionKind kind,
      AnswerStatus answerStatus,
      String failureReason,
      String transcript,
      Long durationMillis,
      Double overall,
      List<ScoreView> scores,
      String strength,
      String improvement,
      String suggestion) {

    static ReportItemView of(ReportQuery.Item it) {
      InterviewQuestion q = it.question();
      Answer a = it.answer();
      Evaluation e = it.evaluation();
      return new ReportItemView(
          q.getSeq(),
          q.getParentSeq(),
          q.getDepth(),
          q.getText(),
          q.getIntent(),
          q.getKind(),
          a == null ? null : a.getStatus(),
          a == null ? null : a.getFailureReason(),
          a == null ? null : a.getTranscript(),
          a == null ? null : a.getDurationMillis(),
          e == null ? null : e.overall(),
          it.scores().entrySet().stream()
              .map(x -> new ScoreView(x.getKey(), x.getValue().score(), x.getValue().evidence()))
              .toList(),
          e == null ? null : e.strength(),
          e == null ? null : e.improvement(),
          e == null ? null : e.suggestion());
    }
  }

  /** 리포트 상태: READY(종합까지 끝) / EVALUATING(진행 중이거나 평가 중) */
  public enum ReportStatus {
    READY,
    EVALUATING
  }

  /** status: READY(종합까지 끝) / EVALUATING(진행 중이거나 평가 중). overall·strength·improvement는 READY일 때만. */
  @Schema(requiredProperties = {"id", "type", "status", "criteria", "items"})
  public record ReportView(
      Long id,
      InterviewType type,
      ReportStatus status,
      Double overall,
      String strength,
      String improvement,
      List<CriterionView> criteria,
      List<ReportItemView> items) {

    static ReportView of(ReportQuery.Report r) {
      SessionReport sum = r.summary();
      return new ReportView(
          r.session().getId(),
          r.session().getType(),
          r.ready() ? ReportStatus.READY : ReportStatus.EVALUATING,
          sum == null ? null : sum.overall(),
          sum == null ? null : sum.strength(),
          sum == null ? null : sum.improvement(),
          r.criteria().stream().map(c -> new CriterionView(c.criterion(), c.average())).toList(),
          r.items().stream().map(ReportItemView::of).toList());
    }
  }
}
