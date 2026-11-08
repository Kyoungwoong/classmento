package com.classmento.embedding;

import java.util.Comparator;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** OpenAI Embeddings API 어댑터 ({@code POST /v1/embeddings}). SDK 없이 RestClient로 직접 호출한다. */
public class OpenAiEmbeddingClient implements EmbeddingClient {

  private final RestClient restClient;
  private final EmbeddingProperties properties;

  public OpenAiEmbeddingClient(EmbeddingProperties properties) {
    this.properties = properties;
    this.restClient =
        RestClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader("Authorization", "Bearer " + properties.apiKey())
            .build();
  }

  @Override
  public List<float[]> embedAll(List<String> texts) {
    if (texts.isEmpty()) {
      return List.of();
    }
    try {
      EmbeddingResponse response =
          restClient
              .post()
              .uri("/embeddings")
              .contentType(MediaType.APPLICATION_JSON)
              .body(new EmbeddingRequest(properties.model(), texts, properties.dimensions()))
              .retrieve()
              .body(EmbeddingResponse.class);
      if (response == null || response.data() == null || response.data().size() != texts.size()) {
        throw new EmbeddingUnavailableException("임베딩 응답 개수가 맞지 않습니다.");
      }
      return response.data().stream()
          .sorted(Comparator.comparingInt(EmbeddingData::index))
          .map(d -> toFloats(d.embedding()))
          .toList();
    } catch (RestClientException e) {
      throw new EmbeddingUnavailableException("OpenAI 임베딩 호출에 실패했습니다.", e);
    }
  }

  @Override
  public int dimensions() {
    return properties.dimensions();
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  private static float[] toFloats(List<Double> values) {
    float[] out = new float[values.size()];
    for (int i = 0; i < out.length; i++) {
      out[i] = values.get(i).floatValue();
    }
    return out;
  }

  record EmbeddingRequest(String model, List<String> input, int dimensions) {}

  record EmbeddingResponse(List<EmbeddingData> data) {}

  record EmbeddingData(int index, List<Double> embedding) {}
}
