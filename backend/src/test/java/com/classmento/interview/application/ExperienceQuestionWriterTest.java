package com.classmento.interview.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.classmento.interview.domain.Experience;
import com.classmento.interview.domain.QuestionDraft;
import com.classmento.interview.domain.QuestionKind;
import com.classmento.llm.LlmClient;
import com.classmento.llm.LlmRequest;
import com.classmento.llm.LlmResponse;
import com.classmento.llm.PromptLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** 경험 기반 질문 만들기: AI 응답 검증·재시도, 실패하면 질문 은행 후보로 대신한다. */
class ExperienceQuestionWriterTest {

  private static final Experience EXP =
      new Experience(
          "실시간 재고 동기화", "Kafka 이벤트로 재고를 맞춤", "주문 이벤트를 Kafka로…", List.of("Kafka"), new float[0]);

  private static final List<QuestionDraft> CANDIDATES =
      List.of(
          QuestionDraft.fromBank(11L, "메시지 큐를 쓰는 이유는요?", "MQ", QuestionKind.TECH),
          QuestionDraft.fromBank(12L, "트랜잭션 격리 수준을 설명해 주세요.", "DB", QuestionKind.TECH),
          QuestionDraft.fromBank(13L, "캐시 정합성은 어떻게 지키나요?", "캐시", QuestionKind.TECH));

  /** 호출마다 answers를 차례로 돌려주는 가짜 LLM */
  private static final class ScriptedLlm implements LlmClient {
    final List<String> answers;
    final List<LlmRequest> calls = new ArrayList<>();
    final boolean available;

    ScriptedLlm(boolean available, String... answers) {
      this.available = available;
      this.answers = List.of(answers);
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
      calls.add(request);
      return new LlmResponse(
          answers.get(Math.min(calls.size() - 1, answers.size() - 1)), "m", 0, 0);
    }

    @Override
    public boolean isAvailable() {
      return available;
    }
  }

  private static ExperienceQuestionWriter writer(ScriptedLlm llm) {
    return new ExperienceQuestionWriter(llm, new PromptLoader(), JsonMapper.builder().build());
  }

  private static final Function<List<QuestionDraft>, List<Long>> BANK_IDS =
      l -> l.stream().map(QuestionDraft::bankQuestionId).toList();

  @Test
  void 경험과_후보로_질문을_만들고_후보에_없는_번호는_버린다() {
    ScriptedLlm llm =
        new ScriptedLlm(
            true,
            """
            ```json
            [{"question":"재고 이벤트를 Kafka로 보낸 이유는 무엇인가요?","intent":"선택 근거","bankQuestionId":11},
             {"question":"이벤트 순서가 뒤바뀌면 재고는 어떻게 맞추셨나요?","intent":"문제 해결","bankQuestionId":999}]
            ```
            """);

    List<QuestionDraft> drafts = writer(llm).write(EXP, CANDIDATES, 2);

    assertThat(drafts)
        .extracting(QuestionDraft::text)
        .containsExactly("재고 이벤트를 Kafka로 보낸 이유는 무엇인가요?", "이벤트 순서가 뒤바뀌면 재고는 어떻게 맞추셨나요?");
    assertThat(drafts).allMatch(QuestionDraft::generated);
    assertThat(BANK_IDS.apply(drafts)).containsExactly(11L, null);
    assertThat(llm.calls.get(0).promptName()).isEqualTo("question-experience.v1");
    assertThat(llm.calls.get(0).userMessage()).contains("<experience>", "실시간 재고 동기화", "[11]");
  }

  @Test
  void JSON이_두_번_깨지면_후보_질문으로_대신한다() {
    ScriptedLlm llm = new ScriptedLlm(true, "질문을 만들 수 없어요", "{not json");

    List<QuestionDraft> drafts = writer(llm).write(EXP, CANDIDATES, 2);

    assertThat(llm.calls).hasSize(2);
    assertThat(BANK_IDS.apply(drafts)).containsExactly(11L, 12L);
    assertThat(drafts).noneMatch(QuestionDraft::generated);
  }

  @Test
  void 모자라게_만들면_쓰지_않은_후보로_채운다() {
    ScriptedLlm llm =
        new ScriptedLlm(
            true,
            "[{\"question\":\"재고 이벤트를 Kafka로 보낸 이유는요?\",\"intent\":\"근거\",\"bankQuestionId\":11},"
                + "{\"question\":\"짧\",\"intent\":\"너무 짧은 질문\",\"bankQuestionId\":null}]");

    List<QuestionDraft> drafts = writer(llm).write(EXP, CANDIDATES, 3);

    assertThat(drafts).hasSize(3);
    assertThat(drafts.get(0).generated()).isTrue();
    assertThat(BANK_IDS.apply(drafts)).containsExactly(11L, 12L, 13L);
  }

  @Test
  void AI를_쓸_수_없으면_부르지_않고_후보로_대신한다() {
    ScriptedLlm llm = new ScriptedLlm(false, "[]");
    assertThat(BANK_IDS.apply(writer(llm).write(EXP, CANDIDATES, 3)))
        .containsExactly(11L, 12L, 13L);
    assertThat(llm.calls).isEmpty();
  }
}
