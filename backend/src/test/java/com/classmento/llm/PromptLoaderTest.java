package com.classmento.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PromptLoaderTest {

  private final PromptLoader loader = new PromptLoader();

  @Test
  void loadsVersionedPrompts() {
    assertThat(loader.load("question-generation.v1")).contains("면접 질문");
    assertThat(loader.load("answer-evaluation.v1")).contains("루브릭");
  }

  @Test
  void failsClearlyForUnknownPrompt() {
    assertThatThrownBy(() -> loader.load("does-not-exist"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does-not-exist");
  }
}
