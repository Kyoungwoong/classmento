package com.classmento.interview.presentation;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classmento.common.security.AuthUser;
import com.classmento.embedding.EmbeddingClient;
import com.classmento.identity.domain.AppUser;
import com.classmento.resume.domain.Resume;
import com.classmento.resume.domain.ResumeExperience;
import com.classmento.resume.domain.ResumeExperienceRepository;
import com.classmento.resume.domain.ResumeRepository;
import com.classmento.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

/** 면접 만들기(기술·인성·혼합은 질문 은행에서 고르기)·조회·목록·권한. 시드 질문 46개 기준. */
class InterviewApiTest extends IntegrationTest {

  private final JsonMapper json = JsonMapper.builder().build();
  private AuthUser me;

  @BeforeEach
  void setUp() {
    me = createUser("me@test.com", AppUser.Role.USER);
  }

  private ResultActions create(String body, AuthUser as) throws Exception {
    return mockMvc.perform(
        post("/api/interviews")
            .with(csrf())
            .with(user(as))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private ResultActions create(String body) throws Exception {
    return create(body, me);
  }

  private long idOf(ResultActions r) throws Exception {
    return json.readTree(r.andReturn().getResponse().getContentAsString()).get("id").asLong();
  }

  /** 질문 문장이 질문 은행 인성 대분류에 있는지 */
  private List<Boolean> behavioralFlags(long sessionId) {
    return jdbc.queryForList(
        "SELECT p.name = '인성' FROM interview_question iq"
            + " JOIN qb_question q ON q.id = iq.bank_question_id"
            + " JOIN qb_category c ON c.id = q.category_id"
            + " JOIN qb_category p ON p.id = c.parent_id"
            + " WHERE iq.session_id = ? ORDER BY iq.seq",
        Boolean.class,
        sessionId);
  }

  @Test
  void 기술_면접은_인성이_아닌_질문을_소분류가_겹치지_않게_고른다() throws Exception {
    long id =
        idOf(
            create("{\"type\":\"TECH\",\"count\":5,\"level\":\"JUNIOR\",\"jobRole\":\"BACKEND\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("TECH"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.questions.length()").value(5))
                .andExpect(jsonPath("$.questions[*].seq", contains(1, 2, 3, 4, 5)))
                .andExpect(jsonPath("$.questions[*].kind", everyItem(is("TECH")))));

    org.assertj.core.api.Assertions.assertThat(behavioralFlags(id)).containsOnly(false);
    Integer distinctSubs =
        jdbc.queryForObject(
            "SELECT count(DISTINCT q.category_id) FROM interview_question iq"
                + " JOIN qb_question q ON q.id = iq.bank_question_id WHERE iq.session_id = ?",
            Integer.class,
            id);
    org.assertj.core.api.Assertions.assertThat(distinctSubs).isEqualTo(5);
  }

  @Test
  void 인성_면접은_인성_질문만() throws Exception {
    long id =
        idOf(
            create("{\"type\":\"BEHAVIORAL\",\"count\":3}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.questions[*].kind", everyItem(is("BEHAVIORAL")))));
    org.assertj.core.api.Assertions.assertThat(behavioralFlags(id)).containsOnly(true);
  }

  @Test
  void 혼합은_기술과_인성을_번갈아_낸다() throws Exception {
    create("{\"type\":\"MIXED\",\"count\":5,\"level\":\"MIDDLE\",\"jobRole\":\"BACKEND\"}")
        .andExpect(status().isCreated())
        .andExpect(
            jsonPath("$.questions[*].kind")
                .value(contains("TECH", "BEHAVIORAL", "TECH", "BEHAVIORAL", "TECH")));
  }

  @Test
  void 조건에_맞는_질문이_모자라면_409() throws Exception {
    create("{\"type\":\"TECH\",\"count\":7,\"level\":\"NEW\",\"jobRole\":\"MOBILE\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("NOT_ENOUGH_QUESTIONS"));
  }

  @Test
  void 조건이_틀리면_409_형식이_틀리면_400() throws Exception {
    create("{\"type\":\"TECH\",\"count\":4,\"level\":\"JUNIOR\",\"jobRole\":\"BACKEND\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_QUESTION_COUNT"));
    create("{\"type\":\"TECH\",\"count\":3}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONDITIONS_REQUIRED"));
    create("{\"count\":3}").andExpect(status().isBadRequest());
  }

  @Autowired ResumeRepository resumes;

  @Test
  void 남의_이력서로_좁히면_404_쓰지_않는_이력서_번호는_저장하지_않는다() throws Exception {
    AuthUser other = createUser("other@test.com", AppUser.Role.USER);
    long theirs = resumes.save(new Resume(other.id(), "r.pdf", "application/pdf", 1, "k")).getId();

    create(
            "{\"type\":\"TECH\",\"count\":3,\"level\":\"JUNIOR\",\"jobRole\":\"BACKEND\","
                + "\"resumeId\":%d,\"narrowByResume\":true}".formatted(theirs))
        .andExpect(status().isNotFound());
    create(
            "{\"type\":\"BEHAVIORAL\",\"count\":3,\"resumeId\":%d,\"experienceId\":1}"
                .formatted(theirs))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.resumeId").doesNotExist())
        .andExpect(jsonPath("$.experienceId").doesNotExist());
  }

  @Test
  void 내_면접만_조회하고_목록은_최신순() throws Exception {
    long first = idOf(create("{\"type\":\"BEHAVIORAL\",\"count\":3}"));
    long second =
        idOf(
            create("{\"type\":\"TECH\",\"count\":3,\"level\":\"JUNIOR\",\"jobRole\":\"BACKEND\"}"));

    mockMvc
        .perform(get("/api/interviews/" + first).with(user(me)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.questions.length()").value(3))
        .andExpect(jsonPath("$.questions[0].text").isNotEmpty());
    mockMvc
        .perform(get("/api/interviews").with(user(me)))
        .andExpect(jsonPath("$[*].id", contains((int) second, (int) first)))
        .andExpect(jsonPath("$[0].questionCount").value(3));

    AuthUser other = createUser("other@test.com", AppUser.Role.USER);
    mockMvc
        .perform(get("/api/interviews/" + first).with(user(other)))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/interviews").with(user(other)))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Autowired ResumeExperienceRepository experiences;
  @Autowired EmbeddingClient embeddings;

  /** 정리된 이력서와 경험 하나를 만들고 [resumeId, experienceId]를 돌려준다. */
  private long[] parsedResumeWithExperience(AuthUser owner) {
    Long resumeId =
        resumes.save(new Resume(owner.id(), "r.pdf", "application/pdf", 1, "k")).getId();
    jdbc.update("UPDATE resume SET status = 'PARSED' WHERE id = ?", resumeId);
    ResumeExperience exp =
        ResumeExperience.create(
            0,
            ResumeExperience.Type.PROJECT,
            "실시간 재고 동기화",
            "커머스",
            "2024",
            "Kafka 이벤트로 주문과 재고를 맞춤",
            "주문 이벤트를 Kafka로 발행하고 재고 서비스가 소비",
            List.of("Kafka", "Spring"));
    experiences.replaceAll(
        resumeId, List.of(exp), embeddings.embedAll(List.of("메시지 큐 Kafka 이벤트 재고")));
    Long expId = experiences.findByResumeId(resumeId).get(0).id();
    return new long[] {resumeId, expId};
  }

  private static String experienceBody(long resumeId, long experienceId) {
    return "{\"type\":\"EXPERIENCE\",\"count\":3,\"resumeId\":%d,\"experienceId\":%d}"
        .formatted(resumeId, experienceId);
  }

  @Test
  void 경험_기반은_경험과_가까운_질문을_바탕으로_AI가_만든_질문을_낸다() throws Exception {
    long[] ids = parsedResumeWithExperience(me);

    long id =
        idOf(
            create(experienceBody(ids[0], ids[1]))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("EXPERIENCE"))
                .andExpect(jsonPath("$.experienceId").value(ids[1]))
                .andExpect(jsonPath("$.questions.length()").value(3))
                .andExpect(jsonPath("$.questions[*].kind", everyItem(is("TECH")))));

    // 가짜 응답(fake/question-experience.v1.json)의 질문은 모두 만든 질문
    Integer generated =
        jdbc.queryForObject(
            "SELECT count(*) FROM interview_question WHERE session_id = ? AND generated",
            Integer.class,
            id);
    org.assertj.core.api.Assertions.assertThat(generated).isEqualTo(3);
  }

  @Test
  void 남의_경험이나_이력서에_없는_경험이면_404() throws Exception {
    AuthUser other = createUser("other@test.com", AppUser.Role.USER);
    long[] theirs = parsedResumeWithExperience(other);
    create(experienceBody(theirs[0], theirs[1])).andExpect(status().isNotFound());

    long[] mine = parsedResumeWithExperience(me);
    create(experienceBody(mine[0], theirs[1])).andExpect(status().isNotFound());
  }

  @Test
  void 로그인해야_한다() throws Exception {
    mockMvc.perform(get("/api/interviews")).andExpect(status().isUnauthorized());
  }
}
