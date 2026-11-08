package com.classmento.speech;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("classmento.stt")
public record SttProperties(String invokeUrl, String secret, String language) {

  public boolean configured() {
    return invokeUrl != null && !invokeUrl.isBlank() && secret != null && !secret.isBlank();
  }
}
