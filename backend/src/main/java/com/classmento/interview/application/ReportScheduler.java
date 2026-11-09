package com.classmento.interview.application;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;

/**
 * 종합 만들기 요청의 유일한 창구. 세션마다 진행 중인 종합은 하나만 둔다 — 리포트 화면의 폴링이나 마지막 평가·끝내기가 겹쳐도 LLM을 여러 번 부르거나 공유 실행기를 채워
 * 다른 사용자의 답변 처리를 막지 않게.
 *
 * <ul>
 *   <li>진행 중에 요청이 오면 버리지 않고 표시만 해 두었다가, 끝난 뒤 한 번 더 돌린다(그 사이 끝난 평가를 놓치지 않게)
 *   <li>작업이 끝나면(성공·실패 모두) 또는 실행기가 거절하면 진행 표시를 지워 다음 요청에서 다시 시도한다
 *   <li>한 서버 안에서만 막는다(서버가 여럿이면 잠금 + 상태 확인으로 저장은 여전히 한 번)
 * </ul>
 */
@Component
public class ReportScheduler {

  private static final Logger log = LoggerFactory.getLogger(ReportScheduler.class);

  private final ReportWriter writer;
  private final TaskExecutor interviewExecutor;
  private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();
  private final Set<Long> pending = ConcurrentHashMap.newKeySet();

  public ReportScheduler(ReportWriter writer, TaskExecutor interviewExecutor) {
    this.writer = writer;
    this.interviewExecutor = interviewExecutor;
  }

  public void request(Long sessionId) {
    if (!inFlight.add(sessionId)) {
      pending.add(sessionId);
      return;
    }
    try {
      interviewExecutor.execute(() -> run(sessionId));
    } catch (TaskRejectedException e) {
      inFlight.remove(sessionId);
    }
  }

  private void run(Long sessionId) {
    try {
      writer.maybeReport(sessionId);
    } catch (RuntimeException e) {
      log.warn("면접 {} 종합 실패: {}", sessionId, e.getClass().getSimpleName());
    } finally {
      inFlight.remove(sessionId);
      if (pending.remove(sessionId)) {
        request(sessionId);
      }
    }
  }
}
