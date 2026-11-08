package com.classmento.resume.presentation;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classmento.common.security.AuthUser;
import com.classmento.embedding.EmbeddingClient;
import com.classmento.identity.domain.AppUser;
import com.classmento.resume.domain.Resume;
import com.classmento.resume.domain.ResumeRepository;
import com.classmento.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** 결과 수정(PUT /parsed)은 본인·상태 확인을 통과한 뒤에만 외부 임베딩(비용)을 호출한다. */
class ResumeUpdateGuardTest extends IntegrationTest {

  private static final String BODY =
      "{\"skills\":[],\"experiences\":[],\"projects\":[{\"name\":\"새 프로젝트\"}],\"educations\":[]}";

  @MockitoSpyBean EmbeddingClient embeddings;
  @Autowired ResumeRepository resumes;

  private Long uploadedResume(Long userId) {
    return resumes.save(new Resume(userId, "cv.pdf", "application/pdf", 10, "k")).getId();
  }

  private Long parsedResume(Long userId) {
    Resume r = new Resume(userId, "cv.pdf", "application/pdf", 10, "k");
    r.markExtracted("본문");
    r.markParsed("{}", "m", "v");
    return resumes.save(r).getId();
  }

  @Test
  void 처리_중인_이력서를_고치면_409이고_임베딩을_부르지_않는다() throws Exception {
    AuthUser me = createUser("me@test.com", AppUser.Role.USER);
    Long id = uploadedResume(me.id());

    mockMvc
        .perform(
            put("/api/resumes/" + id + "/parsed")
                .with(csrf())
                .with(user(me))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isConflict());
    verify(embeddings, never()).embedAll(anyList());
  }

  @Test
  void 남의_이력서를_고치면_404이고_임베딩을_부르지_않는다() throws Exception {
    AuthUser owner = createUser("owner@test.com", AppUser.Role.USER);
    AuthUser other = createUser("other@test.com", AppUser.Role.USER);
    Long id = parsedResume(owner.id());

    mockMvc
        .perform(
            put("/api/resumes/" + id + "/parsed")
                .with(csrf())
                .with(user(other))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isNotFound());
    verify(embeddings, never()).embedAll(anyList());
  }
}
