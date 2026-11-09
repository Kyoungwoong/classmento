package com.classmento.questionbank.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classmento.common.security.AuthUser;
import com.classmento.embedding.EmbeddingClient;
import com.classmento.identity.domain.AppUser;
import com.classmento.questionbank.application.QuestionEmbeddingBackfill;
import com.classmento.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

/** 임베딩을 쓸 수 없을 때(키 없음·외부 장애): 질문은 저장하고 임베딩은 비운다. 예전 벡터를 남기지 않아야 기동 시 채우기가 새 내용으로 다시 만든다. */
class QuestionEmbeddingFallbackTest extends IntegrationTest {

  @MockitoSpyBean EmbeddingClient embeddingClient;
  @Autowired QuestionEmbeddingBackfill backfill;

  private final JsonMapper json = JsonMapper.builder().build();
  private AuthUser admin;
  private long categoryId;

  @BeforeEach
  void setUp() {
    admin = createUser("admin@test.com", AppUser.Role.ADMIN);
    categoryId =
        jdbc.queryForObject(
            "SELECT c.id FROM qb_category c JOIN qb_category p ON c.parent_id = p.id"
                + " WHERE p.name = 'CS' AND c.name = '운영체제'",
            Long.class);
  }

  private String body(String text) {
    return """
        {"categoryId": %d, "text": "%s", "level": "JUNIOR", "jobRoles": ["BACKEND"], "tagIds": []}
        """
        .formatted(categoryId, text);
  }

  private ResultActions create(String text) throws Exception {
    return mockMvc.perform(
        post("/api/admin/questions")
            .with(csrf())
            .with(user(admin))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body(text)));
  }

  private boolean embedded(long id) {
    return Boolean.TRUE.equals(
        jdbc.queryForObject(
            "SELECT embedding IS NOT NULL FROM qb_question WHERE id = ?", Boolean.class, id));
  }

  @Test
  void 임베딩을_쓸_수_없을_때_수정하면_예전_벡터를_비우고_다음_채우기에서_다시_만든다() throws Exception {
    long id =
        json.readTree(create("컨텍스트 스위칭을 설명해 주세요.").andReturn().getResponse().getContentAsString())
            .get("id")
            .asLong();
    assertThat(embedded(id)).isTrue();

    doReturn(false).when(embeddingClient).isAvailable();
    mockMvc
        .perform(
            put("/api/admin/questions/" + id)
                .with(csrf())
                .with(user(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("페이지 교체 알고리즘을 설명해 주세요.")))
        .andExpect(status().isOk());
    assertThat(embedded(id)).isFalse();

    Mockito.reset(embeddingClient);
    backfill.fillMissing();
    assertThat(embedded(id)).isTrue();
  }

  @Test
  void 임베딩_호출이_실패해도_질문은_저장하고_임베딩만_비운다() throws Exception {
    doThrow(new IllegalStateException("embedding down")).when(embeddingClient).embedAll(any());

    long id =
        json.readTree(
                create("스레드 풀을 쓰는 이유를 설명해 주세요.")
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("id")
            .asLong();
    assertThat(embedded(id)).isFalse();
  }
}
