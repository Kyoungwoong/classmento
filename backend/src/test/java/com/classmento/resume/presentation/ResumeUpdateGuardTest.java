package com.classmento.resume.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classmento.common.security.AuthUser;
import com.classmento.embedding.EmbeddingClient;
import com.classmento.embedding.EmbeddingUnavailableException;
import com.classmento.identity.domain.AppUser;
import com.classmento.resume.domain.Resume;
import com.classmento.resume.domain.ResumeExperience;
import com.classmento.resume.domain.ResumeExperience.Type;
import com.classmento.resume.domain.ResumeExperienceRepository;
import com.classmento.resume.domain.ResumeRepository;
import com.classmento.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * 결과 수정(PUT /parsed)·경험 조회의 방어 동작: 본인·상태 확인 뒤에만 외부 임베딩(비용) 호출, 처리 중이면 빈 경험, 임베딩 장애 시 아무것도 바꾸지 않고
 * 503.
 */
class ResumeUpdateGuardTest extends IntegrationTest {

  private static final String BODY =
      "{\"skills\":[],\"experiences\":[],\"projects\":[{\"name\":\"새 프로젝트\"}],\"educations\":[]}";

  @MockitoSpyBean EmbeddingClient embeddings;
  @Autowired ResumeRepository resumes;
  @Autowired ResumeExperienceRepository experiences;

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
  void 파싱이_끝나지_않은_이력서의_경험_목록은_비어_있다() throws Exception {
    AuthUser me = createUser("me@test.com", AppUser.Role.USER);
    Long id = uploadedResume(me.id());

    mockMvc
        .perform(get("/api/resumes/" + id + "/experiences").with(user(me)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void 임베딩이_실패하면_503이고_결과와_경험은_그대로다() throws Exception {
    AuthUser me = createUser("me@test.com", AppUser.Role.USER);
    Long id = parsedResume(me.id());
    ResumeExperience old =
        ResumeExperience.create(0, Type.PROJECT, "옛 경험", null, null, "요약", "본문", List.of());
    experiences.replaceAll(id, List.of(old), embeddings.embedAll(List.of("옛 경험")));

    doThrow(new EmbeddingUnavailableException("down")).when(embeddings).embedAll(anyList());

    mockMvc
        .perform(
            put("/api/resumes/" + id + "/parsed")
                .with(csrf())
                .with(user(me))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("AI_UNAVAILABLE"));

    assertThat(resumes.findById(id).orElseThrow().getParsedJson()).isEqualTo("{}");
    assertThat(experiences.findByResumeId(id))
        .extracting(ResumeExperience::title)
        .containsExactly("옛 경험");
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
