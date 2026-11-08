package com.classmento.common.web;

import com.classmento.common.AiProperties;
import com.classmento.embedding.EmbeddingClient;
import com.classmento.llm.LlmClient;
import com.classmento.speech.SttClient;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

  private final LlmClient llmClient;
  private final SttClient sttClient;
  private final EmbeddingClient embeddingClient;
  private final AiProperties aiProperties;

  public HealthController(
      LlmClient llmClient,
      SttClient sttClient,
      EmbeddingClient embeddingClient,
      AiProperties aiProperties) {
    this.llmClient = llmClient;
    this.sttClient = sttClient;
    this.embeddingClient = embeddingClient;
    this.aiProperties = aiProperties;
  }

  @GetMapping
  public HealthResponse health() {
    return new HealthResponse(
        "ok",
        llmClient.isAvailable(),
        sttClient.isAvailable(),
        embeddingClient.isAvailable(),
        aiProperties.fake());
  }

  /**
   * @param fakeAi true면 키가 없는 AI를 가짜 구현으로 대체 중(로컬 개발)
   */
  @Schema(
      requiredProperties = {
        "status",
        "llmAvailable",
        "sttAvailable",
        "embeddingAvailable",
        "fakeAi"
      })
  public record HealthResponse(
      String status,
      boolean llmAvailable,
      boolean sttAvailable,
      boolean embeddingAvailable,
      boolean fakeAi) {}
}
