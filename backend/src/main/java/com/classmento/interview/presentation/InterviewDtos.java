package com.classmento.interview.presentation;

import com.classmento.interview.application.InterviewService.Command;
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
import java.util.List;

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

  @Schema(requiredProperties = {"seq", "text", "kind"})
  public record InterviewQuestionView(int seq, String text, QuestionKind kind) {

    static InterviewQuestionView of(InterviewQuestion q) {
      return new InterviewQuestionView(q.getSeq(), q.getText(), q.getKind());
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
      return new InterviewView(
          s.getId(),
          s.getType(),
          s.getStatus(),
          s.getLevel(),
          s.getJobRole(),
          s.getResumeId(),
          s.getExperienceId(),
          s.getQuestions().stream().map(InterviewQuestionView::of).toList(),
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
