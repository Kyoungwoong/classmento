package com.classmento.interview.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 답변 처리 실행기 두 개. 테스트(classmento.interview.sync-processing=true)에서는 둘 다 동기 실행.
 *
 * <ul>
 *   <li>{@code answerFastExecutor}: STT → 꼬리질문 판단. 다음 질문이 나오기까지(8초 목표, ADR 0008) 평가·종합에 밀리지 않게 따로
 *       둔다
 *   <li>{@code interviewExecutor}: 평가·종합(LLM, 수 초). 동시 AI 호출 수를 제한한다
 * </ul>
 */
@Configuration
public class InterviewExecutorConfig {

  @Bean
  TaskExecutor answerFastExecutor(
      @Value("${classmento.interview.sync-processing:false}") boolean sync) {
    return sync ? new SyncTaskExecutor() : pool("answer-", 4, 8, 50);
  }

  @Bean
  TaskExecutor interviewExecutor(
      @Value("${classmento.interview.sync-processing:false}") boolean sync) {
    return sync ? new SyncTaskExecutor() : pool("interview-", 2, 4, 200);
  }

  private static TaskExecutor pool(String prefix, int core, int max, int queue) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(core);
    executor.setMaxPoolSize(max);
    executor.setQueueCapacity(queue);
    executor.setThreadNamePrefix(prefix);
    executor.initialize();
    return executor;
  }
}
