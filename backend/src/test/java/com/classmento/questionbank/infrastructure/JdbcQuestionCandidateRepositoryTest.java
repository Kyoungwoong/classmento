package com.classmento.questionbank.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.classmento.embedding.EmbeddingClient;
import com.classmento.questionbank.application.QuestionEmbeddingBackfill;
import com.classmento.questionbank.domain.QuestionCandidateRepository;
import com.classmento.questionbank.domain.QuestionPicker.Candidate;
import com.classmento.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** 경험 기반 면접 후보 kNN(실제 pgvector): 가까운 순, 인성·숨긴 질문·숨긴 카테고리·임베딩 없는 질문 제외. */
class JdbcQuestionCandidateRepositoryTest extends IntegrationTest {

  private static final String TCP = "TCP와 UDP의 차이를 설명하고, 각각 어디에 쓰이는지 말해 주세요.";

  @Autowired QuestionCandidateRepository candidates;
  @Autowired QuestionEmbeddingBackfill backfill;
  @Autowired EmbeddingClient embeddings;

  @BeforeEach
  void fill() {
    backfill.fillMissing();
  }

  private float[] queryOf(String embeddingText) {
    return embeddings.embed(embeddingText);
  }

  private List<String> nearestTexts(float[] q, int k) {
    return candidates.nearestTech(q, k).stream().map(Candidate::text).toList();
  }

  private List<Boolean> isBehavioral(List<Candidate> list) {
    return list.stream()
        .map(
            c ->
                jdbc.queryForObject(
                    "SELECT p.name = '인성' FROM qb_question q JOIN qb_category c ON c.id = q.category_id"
                        + " JOIN qb_category p ON p.id = c.parent_id WHERE q.id = ?",
                    Boolean.class,
                    c.id()))
        .toList();
  }

  @Test
  void 가장_가까운_질문이_먼저_온다() {
    assertThat(nearestTexts(queryOf("CS > 네트워크 · " + TCP), 3).get(0)).isEqualTo(TCP);
  }

  @Test
  void 인성_질문은_가까워도_빠진다() {
    float[] q = queryOf("인성 > 협업 · 다른 직군과 협업하면서 가장 기억에 남는 경험을 말해 주세요.");
    List<Candidate> found = candidates.nearestTech(q, 46);
    assertThat(found).isNotEmpty();
    assertThat(isBehavioral(found)).containsOnly(false);
  }

  @Test
  void 숨긴_질문과_숨긴_카테고리의_질문과_임베딩_없는_질문은_빠진다() {
    float[] q = queryOf("CS > 네트워크 · " + TCP);
    jdbc.update("UPDATE qb_question SET active = FALSE WHERE text = ?", TCP);
    assertThat(nearestTexts(q, 46)).doesNotContain(TCP);

    jdbc.update(
        "UPDATE qb_category SET active = FALSE WHERE name = '운영체제'"
            + " AND parent_id = (SELECT id FROM qb_category WHERE name = 'CS' AND parent_id IS NULL)");
    assertThat(nearestTexts(q, 46)).doesNotContain("프로세스와 스레드의 차이를 설명해 주세요.");

    jdbc.update(
        "UPDATE qb_question SET embedding = NULL WHERE text = ?", "데드락이 생기는 조건과 막는 방법을 설명해 주세요.");
    jdbc.update("UPDATE qb_category SET active = TRUE WHERE name = '운영체제'");
    assertThat(nearestTexts(q, 46)).doesNotContain("데드락이 생기는 조건과 막는 방법을 설명해 주세요.");
  }
}
