package com.classmento.interview.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

/** 종합은 세션마다 하나만 진행한다(폴링·동시 평가 완료로 LLM이 여러 번 불리거나 실행기가 차지 않게). */
class ReportSchedulerTest {

  /** 넣은 작업을 바로 돌리지 않고 쌓아 두는 실행기 */
  private static final class QueueExecutor implements TaskExecutor {
    final List<Runnable> queued = new ArrayList<>();
    boolean reject;

    @Override
    public void execute(Runnable task) {
      if (reject) {
        throw new TaskRejectedException("full");
      }
      queued.add(task);
    }

    void runAll() {
      List<Runnable> now = new ArrayList<>(queued);
      queued.clear();
      now.forEach(Runnable::run);
    }
  }

  private final ReportWriter writer = mock(ReportWriter.class);
  private final QueueExecutor executor = new QueueExecutor();
  private final ReportScheduler scheduler = new ReportScheduler(writer, executor);

  @Test
  void 같은_세션의_종합이_진행_중이면_다시_넣지_않는다() {
    scheduler.request(1L);
    scheduler.request(1L);
    scheduler.request(2L);

    assertThat(executor.queued).hasSize(2);
    executor.runAll();
    verify(writer, times(1)).maybeReport(1L);
    verify(writer, times(1)).maybeReport(2L);
  }

  @Test
  void 끝나면_다시_요청할_수_있다_실패해도() {
    doThrow(new IllegalStateException("db")).when(writer).maybeReport(1L);
    scheduler.request(1L);
    executor.runAll(); // 예외는 삼키고 진행 표시를 지운다

    scheduler.request(1L);
    assertThat(executor.queued).hasSize(1);
  }

  @Test
  void 실행기가_거절하면_진행_표시를_지운다() {
    executor.reject = true;
    scheduler.request(1L);
    executor.reject = false;

    scheduler.request(1L);
    assertThat(executor.queued).hasSize(1);
  }

  @Test
  void 진행_중에_요청이_오면_끝난_뒤_한_번_더_돌린다() {
    // 진행 중인 작업이 답변을 읽은 뒤 마지막 평가가 끝난 경우: 그 요청을 버리면 종합이 빠질 수 있다
    org.mockito.Mockito.doAnswer(
            inv -> {
              scheduler.request(1L); // 작업 도중 들어온 요청
              return null;
            })
        .doNothing()
        .when(writer)
        .maybeReport(1L);

    scheduler.request(1L);
    executor.runAll(); // 첫 작업 → 끝나며 한 번 더 넣음
    assertThat(executor.queued).hasSize(1);
    executor.runAll();
    verify(writer, times(2)).maybeReport(1L);
    assertThat(executor.queued).isEmpty();
  }
}
