package com.classmento.speech;

import com.classmento.common.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SttConfig {

  private static final Logger log = LoggerFactory.getLogger(SttConfig.class);

  /** 우선순위: 실제 키 → (로컬) 가짜 → 사용 불가. LlmConfig와 같은 규칙. */
  @Bean
  SttClient sttClient(SttProperties properties, AiProperties ai) {
    if (properties.configured()) {
      return new ClovaSttClient(properties);
    }
    if (ai.fake()) {
      log.warn("CLOVA_SPEECH 키 없음 → 가짜 STT(DevFakeSttClient) 사용 중. 실제 결과가 아닙니다.");
      return new DevFakeSttClient();
    }
    return new UnavailableSttClient();
  }
}
