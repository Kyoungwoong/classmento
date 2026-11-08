package com.classmento.resume.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.classmento.embedding.EmbeddingClient;
import com.classmento.identity.domain.AppUser;
import com.classmento.resume.domain.Resume;
import com.classmento.resume.domain.ResumeChunkRepository;
import com.classmento.resume.domain.ResumeRepository;
import com.classmento.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** pgvector kNN이 실제로 동작하는지 확인 (테스트는 해시 임베딩 사용). */
class ResumeChunkRepositoryTest extends IntegrationTest {

  @Autowired ResumeChunkRepository chunks;
  @Autowired ResumeRepository resumes;
  @Autowired EmbeddingClient embeddings;

  @Test
  void findNearestReturnsMostSimilarChunkFirst() {
    Long userId = createUser("me@test.com", AppUser.Role.USER).id();
    Long resumeId = resumes.save(new Resume(userId, "r.pdf", "application/pdf", 1, "k")).getId();

    List<String> contents =
        List.of("Redis 캐시로 주문 API 응답 속도 개선", "대학교 컴퓨터공학 전공 졸업", "Kafka 이벤트로 재고 동기화 프로젝트");
    chunks.replaceAll(resumeId, contents, embeddings.embedAll(contents));

    var result = chunks.findNearest(resumeId, embeddings.embed("Kafka 재고 동기화 경험"), 2);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).content()).contains("Kafka");
    assertThat(result.get(0).distance()).isLessThan(result.get(1).distance());
  }
}
