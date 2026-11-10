package com.classmento.interview.application;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;

/**
 * 멈춘 꼬리질문 판단을 다시 돌리는 창구(면접 조회·끝내기 거절 때). 세션마다 진행 중인 복구는 하나만 둔다 — 진행 화면이 1초마다 조회해도 AI를 여러 번 부르지 않게.
 * 진행 중에 요청이 오면 끝난 뒤 한 번 더 돌린다({@link ReportScheduler}와 같은 방식).
 */
@Component
public class FollowUpScheduler {

  private static final Logger log = LoggerFactory.getLogger(FollowUpScheduler.class);

  private final FollowUpService service;
  private final TaskExecutor answerFastExecutor;
  private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();
  private final Set<Long> pending = ConcurrentHashMap.newKeySet();

  public FollowUpScheduler(FollowUpService service, TaskExecutor answerFastExecutor) {
    this.service = service;
    this.answerFastExecutor = answerFastExecutor;
  }

  public void request(Long sessionId) {
    if (!inFlight.add(sessionId)) {
      pending.add(sessionId);
      return;
    }
    try {
      answerFastExecutor.execute(() -> run(sessionId));
    } catch (TaskRejectedException e) {
      inFlight.remove(sessionId);
    }
  }

  private void run(Long sessionId) {
    try {
      service.recover(sessionId);
    } catch (RuntimeException e) {
      log.warn("면접 {} 꼬리질문 복구 실패: {}", sessionId, e.getClass().getSimpleName());
    } finally {
      inFlight.remove(sessionId);
      if (pending.remove(sessionId)) {
        request(sessionId);
      }
    }
  }
}
