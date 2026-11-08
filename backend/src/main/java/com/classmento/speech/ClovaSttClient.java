package com.classmento.speech;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * NAVER CLOVA Speech(장문 인식) 어댑터. {@code POST {invokeUrl}/recognizer/upload}에 동기(sync) 모드로 업로드한다.
 * 상세: docs/architecture.md#외부-연동
 */
public class ClovaSttClient implements SttClient {

  private final RestClient restClient;
  private final SttProperties properties;

  public ClovaSttClient(SttProperties properties) {
    this.properties = properties;
    this.restClient =
        RestClient.builder()
            .baseUrl(properties.invokeUrl())
            .defaultHeader("X-CLOVASpeech-API-Key", properties.secret())
            .build();
  }

  @Override
  public Transcript transcribe(byte[] audio, String filename) {
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add(
        "media",
        new ByteArrayResource(audio) {
          @Override
          public String getFilename() {
            return filename;
          }
        });
    body.add(
        "params",
        "{\"language\":\"%s\",\"completion\":\"sync\",\"wordAlignment\":false}"
            .formatted(properties.language()));
    try {
      ClovaResponse response =
          restClient
              .post()
              .uri("/recognizer/upload")
              .contentType(MediaType.MULTIPART_FORM_DATA)
              .body(body)
              .retrieve()
              .body(ClovaResponse.class);
      if (response == null || !"COMPLETED".equals(response.result())) {
        throw new SttUnavailableException("CLOVA Speech 인식에 실패했습니다.");
      }
      return new Transcript(response.text(), 0);
    } catch (RestClientException e) {
      throw new SttUnavailableException("CLOVA Speech 호출에 실패했습니다.", e);
    }
  }

  @Override
  public boolean isAvailable() {
    return true;
  }

  record ClovaResponse(String result, String message, String text) {}
}
