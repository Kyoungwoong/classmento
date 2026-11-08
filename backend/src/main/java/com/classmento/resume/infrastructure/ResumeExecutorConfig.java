package com.classmento.resume.infrastructure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ResumeExecutorConfig {

  /** 운영: 작은 스레드 풀(동시 AI 호출 수 제한). 테스트: 동기 실행(classmento.resume.sync-processing=true). */
  @Bean
  TaskExecutor resumeExecutor(@Value("${classmento.resume.sync-processing:false}") boolean sync) {
    if (sync) {
      return new SyncTaskExecutor();
    }
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(4);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("resume-");
    executor.initialize();
    return executor;
  }
}
