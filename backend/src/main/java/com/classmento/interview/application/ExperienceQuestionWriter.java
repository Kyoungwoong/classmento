package com.classmento.interview.application;

import com.classmento.interview.domain.Experience;
import com.classmento.interview.domain.QuestionDraft;
import com.classmento.llm.LlmClient;
import com.classmento.llm.PromptLoader;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

/** 경험 기반 면접 질문 만들기. */
public class ExperienceQuestionWriter {

  public ExperienceQuestionWriter(LlmClient llm, PromptLoader prompts, ObjectMapper json) {}

  public List<QuestionDraft> write(Experience exp, List<QuestionDraft> candidates, int count) {
    throw new UnsupportedOperationException("TODO");
  }
}
