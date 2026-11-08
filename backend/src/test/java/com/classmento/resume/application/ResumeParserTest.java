package com.classmento.resume.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classmento.llm.FakeLlmClient;
import com.classmento.llm.PromptLoader;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class ResumeParserTest {

  private final JsonMapper json = JsonMapper.builder().build();

  @Test
  void parsesFencedJsonAndNormalizesNulls() {
    FakeLlmClient llm =
        new FakeLlmClient(
            """
            ```json
            {"profile": {"name": "홍길동"}, "skills": ["Java"], "experiences": null}
            ```
            """);
    ResumeParser.Result result = new ResumeParser(llm, new PromptLoader(), json).parse("이력서 본문");

    assertThat(result.parsed().profile().name()).isEqualTo("홍길동");
    assertThat(result.parsed().experiences()).isEmpty();
    assertThat(result.promptVersion()).isEqualTo("resume-parse.v2");
    assertThat(llm.requests().get(0).userMessage()).contains("<resume>");
  }

  @Test
  void retriesOnceThenFailsOnBrokenJson() {
    FakeLlmClient llm = new FakeLlmClient("이건 JSON이 아니에요");
    ResumeParser parser = new ResumeParser(llm, new PromptLoader(), json);

    assertThatThrownBy(() -> parser.parse("본문")).isInstanceOf(IllegalStateException.class);
    assertThat(llm.requests()).hasSize(2);
  }
}
