package com.classmento.interview.application;

import com.classmento.interview.domain.InterviewLevel;
import com.classmento.interview.domain.InterviewType;
import com.classmento.interview.domain.JobRole;
import com.classmento.interview.domain.QuestionDraft;
import com.classmento.llm.LlmClient;
import com.classmento.llm.PromptLoader;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import tools.jackson.databind.ObjectMapper;

/** 꼬리질문 만들기(follow-up-question 프롬프트). 구현 전 자리(red). */
public class FollowUpWriter {

  public FollowUpWriter(LlmClient llm, PromptLoader prompts, ObjectMapper json, Duration timeout) {}

  public Optional<QuestionDraft> write(Context ctx, List<Turn> chain) {
    return Optional.empty();
  }

  /** 면접 조건과 남은 꼬리 수. */
  public record Context(InterviewType type, JobRole jobRole, InterviewLevel level, int remaining) {}

  /** 사슬의 한 턴: 질문, 의도, 그 질문에 한 답(STT 글자). */
  public record Turn(String question, String intent, String answer) {}
}
