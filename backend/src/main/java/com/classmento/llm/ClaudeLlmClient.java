package com.classmento.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import java.util.stream.Collectors;

/** Claude Messages API 어댑터. 모델 ID는 설정값(classmento.llm.model)으로만 바꾼다. */
public class ClaudeLlmClient implements LlmClient {

  private final AnthropicClient client;
  private final LlmProperties properties;

  public ClaudeLlmClient(LlmProperties properties) {
    this.properties = properties;
    this.client = AnthropicOkHttpClient.builder().apiKey(properties.apiKey()).build();
  }

  @Override
  public LlmResponse generate(LlmRequest request) {
    MessageCreateParams params =
        MessageCreateParams.builder()
            .model(properties.model())
            .maxTokens(properties.maxTokens())
            .system(request.systemPrompt())
            .addUserMessage(request.userMessage())
            .build();
    try {
      Message message = client.messages().create(params);
      String text =
          message.content().stream()
              .flatMap(block -> block.text().stream())
              .map(TextBlock::text)
              .collect(Collectors.joining());
      return new LlmResponse(
          text,
          message.model().asString(),
          message.usage().inputTokens(),
          message.usage().outputTokens());
    } catch (RuntimeException e) {
      throw new LlmUnavailableException("Claude 호출에 실패했습니다.", e);
    }
  }

  @Override
  public boolean isAvailable() {
    return true;
  }
}
