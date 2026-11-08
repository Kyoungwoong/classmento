package com.classmento.resume.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.classmento.embedding.EmbeddingClient;
import com.classmento.identity.domain.AppUser;
import com.classmento.resume.domain.Resume;
import com.classmento.resume.domain.ResumeExperience;
import com.classmento.resume.domain.ResumeExperience.Type;
import com.classmento.resume.domain.ResumeExperienceRepository;
import com.classmento.resume.domain.ResumeRepository;
import com.classmento.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** 경험 저장·조회·kNN을 실제 Postgres(pgvector)로 확인 (테스트는 해시 임베딩). */
class JdbcResumeExperienceRepositoryTest extends IntegrationTest {

  @Autowired ResumeExperienceRepository experiences;
  @Autowired ResumeRepository resumes;
  @Autowired EmbeddingClient embeddings;

  private Long resumeId;

  @BeforeEach
  void setUp() {
    Long userId = createUser("me@test.com", AppUser.Role.USER).id();
    resumeId = resumes.save(new Resume(userId, "r.pdf", "application/pdf", 1, "k")).getId();
  }

  private static ResumeExperience exp(int seq, String title, String summary, List<String> skills) {
    return ResumeExperience.create(seq, Type.PROJECT, title, null, "2023", summary, "본문", skills);
  }

  private void save(List<ResumeExperience> list) {
    experiences.replaceAll(
        resumeId,
        list,
        embeddings.embedAll(list.stream().map(e -> e.title() + " " + e.summary()).toList()));
  }

  @Test
  void 저장한_순서대로_기술까지_되돌려준다() {
    save(
        List.of(
            exp(0, "재고 동기화", "Kafka 이벤트로 재고를 맞춤", List.of("Kafka", "Spring")),
            exp(1, "주문 API", "Redis 캐시로 응답 개선", List.of("Redis"))));

    List<ResumeExperience> found = experiences.findByResumeId(resumeId);

    assertThat(found).extracting(ResumeExperience::title).containsExactly("재고 동기화", "주문 API");
    assertThat(found.get(0).skills()).containsExactly("Kafka", "Spring");
    assertThat(found.get(0).id()).isNotNull();
  }

  @Test
  void 다시_저장하면_이전_경험을_교체한다() {
    save(List.of(exp(0, "옛 경험", "요약", List.of())));
    save(List.of(exp(0, "새 경험", "요약", List.of())));

    assertThat(experiences.findByResumeId(resumeId))
        .extracting(ResumeExperience::title)
        .containsExactly("새 경험");
  }

  @Test
  void 질의와_가장_가까운_경험을_먼저_돌려준다() {
    save(
        List.of(
            exp(0, "주문 API", "Redis 캐시로 응답 개선", List.of()),
            exp(1, "재고 동기화", "Kafka 이벤트로 재고 동기화", List.of()),
            exp(2, "관리자 페이지", "CRUD 화면 개발", List.of())));

    List<ResumeExperience> nearest =
        experiences.findNearest(resumeId, embeddings.embed("Kafka 재고 동기화 경험"), 2);

    assertThat(nearest).hasSize(2);
    assertThat(nearest.get(0).title()).isEqualTo("재고 동기화");
  }
}
