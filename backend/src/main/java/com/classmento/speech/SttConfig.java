package com.classmento.speech;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SttConfig {

  @Bean
  SttClient sttClient(SttProperties properties) {
    return properties.configured() ? new ClovaSttClient(properties) : new UnavailableSttClient();
  }
}
