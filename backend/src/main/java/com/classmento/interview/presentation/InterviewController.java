package com.classmento.interview.presentation;

import com.classmento.common.security.AuthUser;
import com.classmento.interview.application.AnswerService;
import com.classmento.interview.application.InterviewService;
import com.classmento.interview.application.ReportQuery;
import com.classmento.interview.domain.Answer;
import com.classmento.interview.domain.InterviewSession;
import com.classmento.interview.presentation.InterviewDtos.CreateInterviewRequest;
import com.classmento.interview.presentation.InterviewDtos.InterviewSummary;
import com.classmento.interview.presentation.InterviewDtos.InterviewView;
import com.classmento.interview.presentation.InterviewDtos.ReportView;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 면접 세션 (로그인 사용자, 본인 것만 · 남의 것은 404). */
@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

  private final InterviewService service;
  private final AnswerService answers;
  private final ReportQuery reports;

  public InterviewController(InterviewService service, AnswerService answers, ReportQuery reports) {
    this.service = service;
    this.answers = answers;
    this.reports = reports;
  }

  /** 모든 질문에 답했으면 끝낸다. 평가·종합은 백그라운드(진행은 GET …/report). */
  @PostMapping("/{id}/finish")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(operationId = "finishInterview")
  public InterviewView finish(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
    InterviewSession s = answers.finish(user.id(), id);
    return InterviewView.of(s, answers.answersOf(s.getId()));
  }

  @GetMapping("/{id}/report")
  @Operation(operationId = "getInterviewReport")
  public ReportView report(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
    return ReportView.of(reports.report(user.id(), id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(operationId = "createInterview")
  public InterviewView create(
      @Valid @RequestBody CreateInterviewRequest body, @AuthenticationPrincipal AuthUser user) {
    return InterviewView.of(service.create(user.id(), body.toCommand()));
  }

  @GetMapping
  @Operation(operationId = "listInterviews")
  public List<InterviewSummary> list(@AuthenticationPrincipal AuthUser user) {
    return service.list(user.id()).stream().map(InterviewSummary::of).toList();
  }

  @GetMapping("/{id}")
  @Operation(operationId = "getInterview")
  public InterviewView get(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
    InterviewSession s = service.get(user.id(), id);
    List<Answer> list = answers.answersForProgress(s);
    // 복구가 동기로 끝났으면(테스트·빠른 응답) 새 꼬리를 바로 보여 준다
    return InterviewView.of(service.get(user.id(), id), list);
  }
}
