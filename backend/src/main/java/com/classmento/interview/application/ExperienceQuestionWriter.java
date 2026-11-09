package com.classmento.interview.application;

import com.classmento.interview.domain.Experience;
import com.classmento.interview.domain.QuestionDraft;
import com.classmento.interview.domain.QuestionKind;
import com.classmento.llm.LlmClient;
import com.classmento.llm.LlmRequest;
import com.classmento.llm.LlmUnavailableException;
import com.classmento.llm.PromptLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 경험 기반 면접 질문 만들기(question-experience 프롬프트). 경험 원문 + 질문 은행 kNN 후보를 주고 질문을 받는다.
 *
 * <ul>
 *   <li>질문이 5~300자가 아니면 버리고, 후보에 없는 번호는 지운다(만든 질문으로 본다)
 *   <li>JSON이 깨지면 한 번 더 부른다
 *   <li>그래도 모자라면 아직 쓰지 않은 후보 질문으로 채운다(AI를 쓸 수 없을 때도 후보로)
 * </ul>
 */
@Component
public class ExperienceQuestionWriter {

  static final String PROMPT = "question-experience.v1";
  private static final int TEXT_MIN = 5;
  private static final int TEXT_MAX = 300;
  private static final int INTENT_MAX = 200;
  private static final Logger log = LoggerFactory.getLogger(ExperienceQuestionWriter.class);

  private final LlmClient llm;
  private final PromptLoader prompts;
  private final ObjectMapper json;

  public ExperienceQuestionWriter(LlmClient llm, PromptLoader prompts, ObjectMapper json) {
    this.llm = llm;
    this.prompts = prompts;
    this.json = json;
  }

  public List<QuestionDraft> write(Experience exp, List<QuestionDraft> candidates, int count) {
    List<QuestionDraft> written = llm.isAvailable() ? ask(exp, candidates, count) : List.of();
    List<QuestionDraft> out = new ArrayList<>(written.subList(0, Math.min(count, written.size())));
    Set<Long> used = new HashSet<>();
    out.forEach(d -> used.add(d.bankQuestionId()));
    for (QuestionDraft c : candidates) {
      if (out.size() < count && !used.contains(c.bankQuestionId())) {
        out.add(c);
        used.add(c.bankQuestionId());
      }
    }
    return out;
  }

  private List<QuestionDraft> ask(Experience exp, List<QuestionDraft> candidates, int count) {
    LlmRequest request =
        new LlmRequest(PROMPT, prompts.load(PROMPT), message(exp, candidates, count));
    Set<Long> known = new HashSet<>();
    candidates.forEach(c -> known.add(c.bankQuestionId()));
    for (int attempt = 0; attempt < 2; attempt++) {
      try {
        Item[] items = json.readValue(stripFence(llm.generate(request).text()), Item[].class);
        return valid(items, known);
      } catch (JacksonException e) {
        log.warn("경험 질문 응답(JSON)을 읽지 못함({}번째): {}", attempt + 1, e.getOriginalMessage());
      } catch (LlmUnavailableException e) {
        log.warn("경험 질문 생성 실패, 질문 은행 후보로 대신: {}", e.getMessage());
        return List.of();
      }
    }
    return List.of();
  }

  private static List<QuestionDraft> valid(Item[] items, Set<Long> known) {
    List<QuestionDraft> out = new ArrayList<>();
    Set<Long> used = new HashSet<>();
    for (Item it : items) {
      String q = it.question() == null ? "" : it.question().strip();
      if (q.length() < TEXT_MIN || q.length() > TEXT_MAX) {
        continue;
      }
      Long bank =
          it.bankQuestionId() != null
                  && known.contains(it.bankQuestionId())
                  && used.add(it.bankQuestionId())
              ? it.bankQuestionId()
              : null;
      String intent = it.intent() == null ? null : it.intent().strip();
      if (intent != null && intent.length() > INTENT_MAX) {
        intent = intent.substring(0, INTENT_MAX);
      }
      out.add(new QuestionDraft(bank, q, intent, QuestionKind.TECH, true));
    }
    return out;
  }

  private static String message(Experience exp, List<QuestionDraft> candidates, int count) {
    StringBuilder sb = new StringBuilder("<experience>\n");
    sb.append("제목: ").append(exp.title()).append('\n');
    sb.append("요약: ").append(exp.summary()).append('\n');
    if (exp.detail() != null && !exp.detail().isBlank()) {
      sb.append("본문: ").append(exp.detail()).append('\n');
    }
    sb.append("기술: ").append(String.join(", ", exp.skills())).append("\n</experience>\n");
    sb.append("<candidates>\n");
    for (QuestionDraft c : candidates) {
      sb.append('[').append(c.bankQuestionId()).append("] ").append(c.text()).append('\n');
    }
    sb.append("</candidates>\n<count>").append(count).append("</count>");
    return sb.toString();
  }

  /** 모델이 ```json ... ``` 으로 감싸 답한 경우 벗긴다. */
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

  private record Item(String question, String intent, Long bankQuestionId) {}
}
