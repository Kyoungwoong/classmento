package com.classmento.interview.application;

import org.springframework.core.task.TaskExecutor;

/** 종합 만들기 요청 창구. */
public class ReportScheduler {

  public ReportScheduler(ReportWriter writer, TaskExecutor interviewExecutor) {}

  public void request(Long sessionId) {}
}
