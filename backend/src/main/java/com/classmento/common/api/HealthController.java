package com.classmento.common.api;

import com.classmento.llm.LlmClient;
import com.classmento.speech.SttClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

  private final LlmClient llmClient;
  private final SttClient sttClient;

  public HealthController(LlmClient llmClient, SttClient sttClient) {
    this.llmClient = llmClient;
    this.sttClient = sttClient;
  }

  @GetMapping
  public HealthResponse health() {
    return new HealthResponse("ok", llmClient.isAvailable(), sttClient.isAvailable());
  }

  public record HealthResponse(String status, boolean llmAvailable, boolean sttAvailable) {}
}
