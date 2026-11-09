package com.classmento.interview.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class InterviewExecutorConfig {

  /**
   * 답변 STT·평가용. 운영: 작은 스레드 풀(동시 AI 호출 수 제한). 테스트: 동기 실행
   * (classmento.interview.sync-processing=true).
   */
  @Bean
  TaskExecutor interviewExecutor(
      @Value("${classmento.interview.sync-processing:false}") boolean sync) {
    if (sync) {
      return new SyncTaskExecutor();
    }
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(200);
    executor.setThreadNamePrefix("interview-");
    executor.initialize();
    return executor;
  }
}
