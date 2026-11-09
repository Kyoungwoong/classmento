package com.classmento.interview.presentation;

import com.classmento.interview.application.InterviewService.Command;
import com.classmento.interview.domain.Answer;
import com.classmento.interview.domain.AnswerStatus;
import com.classmento.interview.domain.InterviewLevel;
import com.classmento.interview.domain.InterviewQuestion;
import com.classmento.interview.domain.InterviewSession;
import com.classmento.interview.domain.InterviewType;
import com.classmento.interview.domain.JobRole;
import com.classmento.interview.domain.QuestionKind;
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
      Boolean narrowByResume) {

    Command toCommand() {
      return new Command(
          type, count, level, jobRole, resumeId, experienceId, Boolean.TRUE.equals(narrowByResume));
    }
  }

  /** answerStatus: 답하지 않았으면 없음. failureReason: 실패했을 때 원인과 할 일. */
  @Schema(requiredProperties = {"seq", "text", "kind"})
  public record InterviewQuestionView(
      int seq, String text, QuestionKind kind, AnswerStatus answerStatus, String failureReason) {

    static InterviewQuestionView of(InterviewQuestion q, Answer a) {
      return new InterviewQuestionView(
          q.getSeq(),
          q.getText(),
          q.getKind(),
          a == null ? null : a.getStatus(),
          a == null ? null : a.getFailureReason());
    }
  }

  @Schema(requiredProperties = {"seq", "status"})
  public record AnswerStatusView(int seq, AnswerStatus status) {

    static AnswerStatusView of(Answer a) {
      return new AnswerStatusView(a.getSeq(), a.getStatus());
    }
  }

  @Schema(requiredProperties = {"id", "type", "status", "questions", "createdAt"})
  public record InterviewView(
      Long id,
      InterviewType type,
      SessionStatus status,
      InterviewLevel level,
      JobRole jobRole,
      Long resumeId,
      Long experienceId,
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
          s.getId(), s.getType(), s.getStatus(), s.getQuestions().size(), s.getCreatedAt());
    }
  }
}
