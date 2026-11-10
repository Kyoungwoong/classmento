package com.classmento.interview.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 면접 처리 설정({@code classmento.interview}).
 *
 * @param syncProcessing true면 답변 STT·꼬리질문·평가를 요청 안에서 바로 처리(테스트용)
 * @param followUpTimeout 꼬리질문 AI 호출 제한 시간(다음 질문까지 8초 목표, ADR 0008). 기본 5초
 * @param followUpConcurrency 동시에 진행할 꼬리질문 AI 호출 수 상한(넘치면 꼬리 없이 넘어간다). 기본 8
 */
@ConfigurationProperties("classmento.interview")
public record InterviewProperties(
    boolean syncProcessing, Duration followUpTimeout, Integer followUpConcurrency) {

  public InterviewProperties {
    if (followUpTimeout == null) {
      followUpTimeout = Duration.ofSeconds(5);
    }
    if (followUpConcurrency == null || followUpConcurrency < 1) {
      followUpConcurrency = 8;
    }
  }
}
