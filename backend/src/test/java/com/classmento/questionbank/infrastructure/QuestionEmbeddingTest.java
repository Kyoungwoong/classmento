package com.classmento.questionbank.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.classmento.embedding.EmbeddingClient;
import com.classmento.questionbank.application.QuestionEmbeddingBackfill;
import com.classmento.questionbank.domain.QuestionEmbeddingRepository;
import com.classmento.questionbank.domain.QuestionEmbeddingRepository.Match;
import com.classmento.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** 질문 임베딩: 기동 시 비어 있는 임베딩 채우기, 전역 kNN(보이는 질문만). 테스트는 해시 임베딩. */
class QuestionEmbeddingTest extends IntegrationTest {

  @Autowired QuestionEmbeddingBackfill backfill;
  @Autowired QuestionEmbeddingRepository embeddings;
  @Autowired EmbeddingClient embeddingClient;

  @Test
  void 비어_있는_시드_질문_임베딩을_채운다() {
    jdbc.update("UPDATE qb_question SET embedding = NULL WHERE seeded");

    int filled = backfill.fillMissing();

    assertThat(filled).isEqualTo(46);
    Integer missing =
        jdbc.queryForObject(
            "SELECT count(*) FROM qb_question WHERE embedding IS NULL", Integer.class);
    assertThat(missing).isZero();
  }

  @Test
  void 가장_비슷한_질문을_먼저_돌려주고_숨긴_질문은_빼다() {
    backfill.fillMissing();
    float[] query = embeddingClient.embed("CS > 네트워크 · TCP와 UDP의 차이를 설명하고, 각각 어디에 쓰이는지 말해 주세요.");

    List<Match> nearest = embeddings.findNearest(query, 3);
    assertThat(nearest).hasSize(3);
    assertThat(nearest.get(0).text()).contains("TCP와 UDP");

    jdbc.update("UPDATE qb_question SET active = FALSE WHERE id = ?", nearest.get(0).questionId());
    assertThat(embeddings.findNearest(query, 3))
        .extracting(Match::questionId)
        .doesNotContain(nearest.get(0).questionId());
  }
}
