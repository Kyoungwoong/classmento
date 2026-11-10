package com.classmento.interview.application;

import com.classmento.interview.domain.InterviewLevel;
import com.classmento.interview.domain.InterviewType;
import com.classmento.interview.domain.JobRole;
import com.classmento.interview.domain.QuestionDraft;
import com.classmento.interview.domain.QuestionKind;
import com.classmento.llm.LlmClient;
import com.classmento.llm.LlmRequest;
import com.classmento.llm.PromptLoader;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 꼬리질문 만들기(follow-up-question 프롬프트). 직전 답에서 한 번 더 파고드는 기술 질문 하나를 받는다(ADR 0008).
 *
 * <ul>
 *   <li>AI가 고른 근거({@code basis})가 직전 답 안에 실제로 있어야 받는다(공백만 다르면 같은 말로 본다) — 답에 없는 내용을 지어 묻지 않게
 *   <li>질문이 5~300자가 아니거나 사슬에 이미 있는 질문이면 받지 않는다
 *   <li>다음 질문까지 8초 목표라 시간 제한을 두고, JSON이 깨져도 다시 부르지 않는다
 *   <li>어떤 실패든 빈 결과("꼬리 없음")로 돌려준다. 면접은 멈추지 않는다
 * </ul>
 */
@Component
public class FollowUpWriter {

  static final String PROMPT = "follow-up-question.v1";
  private static final int TEXT_MIN = 5;
  private static final int TEXT_MAX = 300;
  private static final int INTENT_MAX = 200;

  /** 근거 최소 길이(공백 정리 뒤). "캐시"처럼 짧으면 어느 답에나 들어 있어 근거가 되지 못한다. */
  static final int BASIS_MIN = 6;

  private static final Logger log = LoggerFactory.getLogger(FollowUpWriter.class);

  private final LlmClient llm;
  private final PromptLoader prompts;
  private final ObjectMapper json;
  private final Duration timeout;

  /**
   * 시간 제한을 걸기 위해 LLM 호출만 따로 돌리는 스레드. 크기를 묶어 둔다: 제한 시간이 지난 호출은 결과를 버리지만 HTTP는 클라이언트 타임아웃까지 스레드를
   * 잡으므로, AI가 느려져도 스레드·동시 호출이 끝없이 늘지 않게(넘치면 꼬리 없이 넘어간다).
   */
  private final ExecutorService calls;

  @Autowired
  public FollowUpWriter(
      LlmClient llm, PromptLoader prompts, ObjectMapper json, InterviewProperties props) {
    this(llm, prompts, json, props.followUpTimeout(), props.followUpConcurrency());
  }

  FollowUpWriter(LlmClient llm, PromptLoader prompts, ObjectMapper json, Duration timeout) {
    this(llm, prompts, json, timeout, InterviewProperties.DEFAULT_FOLLOW_UP_CONCURRENCY);
  }

  private FollowUpWriter(
      LlmClient llm, PromptLoader prompts, ObjectMapper json, Duration timeout, int concurrency) {
    this.llm = llm;
    this.prompts = prompts;
    this.json = json;
    this.timeout = timeout;
    this.calls =
        new ThreadPoolExecutor(
            concurrency,
            concurrency,
            30,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            r -> {
              Thread t = new Thread(r, "follow-up-llm");
              t.setDaemon(true);
              return t;
            },
            new ThreadPoolExecutor.AbortPolicy());
    ((ThreadPoolExecutor) this.calls).allowCoreThreadTimeOut(true);
  }

  @PreDestroy
  void shutdown() {
    calls.shutdownNow();
  }

  /**
   * @param chain 본 질문부터 지금 질문까지(마지막 턴의 답을 근거로 묻는다)
   * @return 꼬리질문, 붙이지 않으면 빈 값
   */
  public Optional<QuestionDraft> write(Context ctx, List<Turn> chain) {
    if (chain.isEmpty() || !llm.isAvailable()) {
      return Optional.empty();
    }
    LlmRequest request = new LlmRequest(PROMPT, prompts.load(PROMPT), message(ctx, chain));
    String text;
    CompletableFuture<String> call;
    try {
      call = CompletableFuture.supplyAsync(() -> llm.generate(request).text(), calls);
    } catch (RejectedExecutionException e) {
      log.warn("꼬리질문 AI 호출이 몰려 건너뜀");
      return Optional.empty();
    }
    try {
      text = call.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      call.cancel(true);
      log.warn("꼬리질문 생성이 {}ms 안에 끝나지 않아 건너뜀", timeout.toMillis());
      return Optional.empty();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    } catch (Exception e) {
      Throwable cause = e.getCause() == null ? e : e.getCause();
      log.warn("꼬리질문 생성 실패, 건너뜀: {}", cause.getClass().getSimpleName());
      return Optional.empty();
    }
    try {
      return valid(json.readValue(stripFence(text), Item.class), chain);
    } catch (JacksonException e) {
      // 응답 본문(답변 내용이 섞일 수 있음)은 로그에 남기지 않는다
      log.warn("꼬리질문 응답(JSON)을 읽지 못해 건너뜀: {}", e.getClass().getSimpleName());
      return Optional.empty();
    }
  }

  private static Optional<QuestionDraft> valid(Item it, List<Turn> chain) {
    if (it == null || it.done()) {
      return Optional.empty();
    }
    String lastAnswer = normalize(chain.get(chain.size() - 1).answer());
    String basis = normalize(it.basis());
    if (basis.length() < BASIS_MIN || !lastAnswer.contains(basis)) {
      return Optional.empty();
    }
    String q = it.question() == null ? "" : it.question().strip();
    if (q.length() < TEXT_MIN || q.length() > TEXT_MAX) {
      return Optional.empty();
    }
    String key = normalize(q);
    if (chain.stream().anyMatch(t -> normalize(t.question()).equals(key))) {
      return Optional.empty();
    }
    String intent = it.intent() == null ? null : it.intent().strip();
    if (intent != null && intent.length() > INTENT_MAX) {
      intent = intent.substring(0, INTENT_MAX);
    }
    return Optional.of(QuestionDraft.generated(q, intent, QuestionKind.TECH));
  }

  private static String message(Context ctx, List<Turn> chain) {
    StringBuilder sb = new StringBuilder("<interview>\n");
    sb.append("유형: ").append(ctx.type()).append('\n');
    if (ctx.jobRole() != null) {
      sb.append("직무: ").append(ctx.jobRole()).append('\n');
    }
    if (ctx.level() != null) {
      sb.append("연차: ").append(ctx.level()).append('\n');
    }
    sb.append("</interview>\n<chain>\n");
    for (int i = 0; i < chain.size(); i++) {
      Turn t = chain.get(i);
      sb.append("<turn depth=\"").append(i).append("\">\n");
      sb.append("<question>").append(escape(t.question())).append("</question>\n");
      if (t.intent() != null && !t.intent().isBlank()) {
        sb.append("<intent>").append(escape(t.intent())).append("</intent>\n");
      }
      sb.append("<answer>").append(escape(t.answer())).append("</answer>\n");
      sb.append("</turn>\n");
    }
    sb.append("</chain>\n<remaining>").append(ctx.remaining()).append("</remaining>");
    return sb.toString();
  }

  private static String normalize(String s) {
    return s == null ? "" : s.replaceAll("\\s+", " ").strip();
  }

  private static String escape(String s) {
    return s == null ? "" : s.replace("<", "‹").replace(">", "›");
  }

  /** 모델이 ```json ... ``` 으로 감싸 답한 경우 벗긴다. */
  private static String stripFence(String s) {
    String t = s == null ? "" : s.strip();
    if (t.startsWith("```")) {
      int firstNewline = t.indexOf('\n');
      int lastFence = t.lastIndexOf("```");
      if (firstNewline > 0 && lastFence > firstNewline) {
        return t.substring(firstNewline + 1, lastFence).strip();
      }
    }
    return t;
  }

  /** 면접 조건과 남은 꼬리 수. */
  public record Context(InterviewType type, JobRole jobRole, InterviewLevel level, int remaining) {}

  /** 사슬의 한 턴: 질문, 의도, 그 질문에 한 답(STT 글자). */
  public record Turn(String question, String intent, String answer) {}

  private record Item(boolean done, String basis, String question, String intent) {}
}
