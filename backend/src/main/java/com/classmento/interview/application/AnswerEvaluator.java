package com.classmento.interview.application;

import com.classmento.common.domain.DomainException;
import com.classmento.interview.domain.Evaluation;
import com.classmento.interview.domain.InterviewQuestion;
import com.classmento.interview.domain.Rubric;
import com.classmento.interview.domain.Score;
import com.classmento.llm.LlmClient;
import com.classmento.llm.LlmRequest;
import com.classmento.llm.LlmResponse;
import com.classmento.llm.PromptLoader;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** 질문 + STT 답변 → 루브릭 평가(answer-evaluation 프롬프트). JSON이 깨지거나 규칙에 맞지 않으면 한 번 다시 부르고, 그래도 안 되면 예외. */
@Component
public class AnswerEvaluator {

  static final String PROMPT = "answer-evaluation.v2";

  private final LlmClient llm;
  private final PromptLoader prompts;
  private final ObjectMapper json;

  public AnswerEvaluator(LlmClient llm, PromptLoader prompts, ObjectMapper json) {
    this.llm = llm;
    this.prompts = prompts;
    this.json = json;
  }

  public Evaluation evaluate(InterviewQuestion question, String transcript) {
    LlmRequest request =
        new LlmRequest(PROMPT, prompts.load(PROMPT), message(question, transcript));
    RuntimeException last = null;
    for (int attempt = 0; attempt < 2; attempt++) {
      LlmResponse response = llm.generate(request);
      try {
        Output out = json.readValue(stripFence(response.text()), Output.class);
        Map<String, Score> scores = new HashMap<>();
        if (out.scores() != null) {
          out.scores()
              .forEach(
                  (k, v) -> scores.put(k, v == null ? null : new Score(v.score(), v.evidence())));
        }
        return Evaluation.of(
            question.getKind(),
            scores,
            out.strength(),
            out.improvement(),
            out.suggestion(),
            PROMPT,
            response.model());
      } catch (JacksonException | DomainException e) {
        last = e;
      }
    }
    throw new IllegalStateException("답변 평가 결과를 해석하지 못했습니다.", last);
  }

  private static String message(InterviewQuestion q, String transcript) {
    StringBuilder sb = new StringBuilder();
    sb.append("<question>").append(q.getText()).append("</question>\n");
    if (q.getIntent() != null) {
      sb.append("<intent>").append(q.getIntent()).append("</intent>\n");
    }
    sb.append("<rubric>").append(Rubric.of(q.getKind()).name()).append("</rubric>\n");
    // 답변 안의 "</answer>" 같은 글자가 태그 경계를 깨지 않게 꺾쇠를 바꾼다
    sb.append("<answer>\n").append(escape(transcript)).append("\n</answer>");
    return sb.toString();
  }

  private static String escape(String s) {
    return s.replace("<", "‹").replace(">", "›");
  }

  private static String stripFence(String s) {
    String t = s.strip();
    if (t.startsWith("```")) {
      int firstNewline = t.indexOf('\n');
      int lastFence = t.lastIndexOf("```");
      if (firstNewline > 0 && lastFence > firstNewline) {
        return t.substring(firstNewline + 1, lastFence).strip();
      }
    }
    return t;
  }

  private record RawScore(int score, String evidence) {}

  private record Output(
      Map<String, RawScore> scores, String strength, String improvement, String suggestion) {}
}
