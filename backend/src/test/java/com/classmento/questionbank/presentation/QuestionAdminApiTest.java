package com.classmento.questionbank.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classmento.common.security.AuthUser;
import com.classmento.identity.domain.AppUser;
import com.classmento.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

/** 관리자 질문 API: 등록·검증·규칙·검색(필터·페이지)·수정·숨김·권한·임베딩. 시드: 소분류마다 질문 2개. */
class QuestionAdminApiTest extends IntegrationTest {

  private final JsonMapper json = JsonMapper.builder().build();
  private AuthUser admin;

  @BeforeEach
  void setUp() {
    admin = createUser("admin@test.com", AppUser.Role.ADMIN);
  }

  private long categoryId(String top, String sub) {
    return jdbc.queryForObject(
        "SELECT c.id FROM qb_category c JOIN qb_category p ON c.parent_id = p.id"
            + " WHERE p.name = ? AND c.name = ?",
        Long.class,
        top,
        sub);
  }

  private long topId(String name) {
    return jdbc.queryForObject(
        "SELECT id FROM qb_category WHERE parent_id IS NULL AND name = ?", Long.class, name);
  }

  private long tag(String name) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/api/admin/tags")
                    .with(csrf())
                    .with(user(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"" + name + "\"}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(body).get("tag").get("id").asLong();
  }

  private ResultActions create(String body) throws Exception {
    return mockMvc.perform(
        post("/api/admin/questions")
            .with(csrf())
            .with(user(admin))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private String body(long categoryId, String text, String tagIds) {
    return """
        {"categoryId": %d, "text": "%s", "level": "JUNIOR",
         "jobRoles": ["BACKEND"], "tagIds": [%s], "intent": "의도"}
        """
        .formatted(categoryId, text, tagIds);
  }

  private long idOf(ResultActions r) throws Exception {
    return json.readTree(r.andReturn().getResponse().getContentAsString()).get("id").asLong();
  }

  @Test
  void 질문을_등록하면_카테고리_태그_이름과_함께_돌려주고_임베딩을_채운다() throws Exception {
    long jpa = tag("JPA");
    long id =
        idOf(
            create(body(categoryId("Framework", "Spring"), "영속성 컨텍스트의 1차 캐시를 설명해 주세요.", "" + jpa))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topCategoryName").value("Framework"))
                .andExpect(jsonPath("$.categoryName").value("Spring"))
                .andExpect(jsonPath("$.tags[0].name").value("JPA"))
                .andExpect(jsonPath("$.jobRoles[0]").value("BACKEND"))
                .andExpect(jsonPath("$.active").value(true)));

    Integer embedded =
        jdbc.queryForObject(
            "SELECT count(*) FROM qb_question WHERE id = ? AND embedding IS NOT NULL",
            Integer.class,
            id);
    assertThat(embedded).isEqualTo(1);
  }

  @Test
  void 대분류_숨긴_카테고리는_409_없는_카테고리는_404_없는_태그는_400() throws Exception {
    create(body(topId("CS"), "대분류에 넣으려는 질문입니다", ""))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("QUESTION_NEEDS_SUBCATEGORY"));

    long network = categoryId("CS", "네트워크");
    jdbc.update("UPDATE qb_category SET active = FALSE WHERE id = ?", network);
    create(body(network, "숨긴 소분류에 넣으려는 질문", ""))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CATEGORY_HIDDEN"));

    create(body(999999, "없는 카테고리의 질문입니다", "")).andExpect(status().isNotFound());
    create(body(categoryId("CS", "보안"), "없는 태그를 붙인 질문입니다", "999999"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 입력_형식_오류는_400() throws Exception {
    long cat = categoryId("CS", "보안");
    create(body(cat, "짧아", "")).andExpect(status().isBadRequest());
    create(
            """
            {"categoryId": %d, "text": "직무가 없는 질문입니다", "level": "NEW", "jobRoles": [], "tagIds": []}
            """
                .formatted(cat))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 기본_목록은_보이는_질문을_최신순으로_페이지_단위로() throws Exception {
    mockMvc
        .perform(get("/api/admin/questions").param("size", "5").with(user(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(5))
        .andExpect(jsonPath("$.total").value(46))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(5));
  }

  @Test
  void 대분류_소분류_난이도_직무_검색어로_거른다() throws Exception {
    mockMvc
        .perform(
            get("/api/admin/questions")
                .param("categoryId", "" + categoryId("Language", "Java"))
                .with(user(admin)))
        .andExpect(jsonPath("$.total").value(2));

    mockMvc
        .perform(
            get("/api/admin/questions")
                .param("topCategoryId", "" + topId("인성"))
                .param("level", "JUNIOR")
                .with(user(admin)))
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(
            jsonPath("$.items[*].topCategoryName", everyItem(org.hamcrest.Matchers.is("인성"))));

    mockMvc
        .perform(
            get("/api/admin/questions")
                .param("jobRole", "DEVOPS")
                .param("q", "배포")
                .with(user(admin)))
        .andExpect(jsonPath("$.total").value(1));
  }

  @Test
  void 태그로_거르고_숨긴_질문은_숨김_보기에서만() throws Exception {
    long redis = tag("Redis");
    long id =
        idOf(create(body(categoryId("설계·인프라", "시스템 설계"), "Redis 캐시 만료 전략을 말해 주세요.", "" + redis)));

    mockMvc
        .perform(get("/api/admin/questions").param("tagId", "" + redis).with(user(admin)))
        .andExpect(jsonPath("$.total").value(1));

    mockMvc
        .perform(
            patch("/api/admin/questions/" + id + "/active")
                .with(csrf())
                .with(user(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.active").value(false));

    mockMvc
        .perform(get("/api/admin/questions").param("tagId", "" + redis).with(user(admin)))
        .andExpect(jsonPath("$.total").value(0));
    mockMvc
        .perform(
            get("/api/admin/questions")
                .param("tagId", "" + redis)
                .param("includeHidden", "true")
                .with(user(admin)))
        .andExpect(jsonPath("$.total").value(1));
  }

  @Test
  void 수정하면_내용과_임베딩이_바뀐다() throws Exception {
    long id = idOf(create(body(categoryId("CS", "운영체제"), "컨텍스트 스위칭을 설명해 주세요.", "")));
    String before =
        jdbc.queryForObject(
            "SELECT embedding::text FROM qb_question WHERE id = ?", String.class, id);

    mockMvc
        .perform(
            put("/api/admin/questions/" + id)
                .with(csrf())
                .with(user(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(categoryId("CS", "네트워크"), "TCP 혼잡 제어를 설명해 주세요.", "")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categoryName").value("네트워크"))
        .andExpect(jsonPath("$.text").value("TCP 혼잡 제어를 설명해 주세요."));

    String after =
        jdbc.queryForObject(
            "SELECT embedding::text FROM qb_question WHERE id = ?", String.class, id);
    assertThat(after).isNotEqualTo(before);
  }

  @Test
  void 카테고리_트리에_소분류별_질문_수가_있다() throws Exception {
    mockMvc
        .perform(get("/api/admin/categories").with(user(admin)))
        .andExpect(jsonPath("$[0].children[0].name").value("Java"))
        .andExpect(jsonPath("$[0].children[0].questionCount").value(2))
        .andExpect(jsonPath("$[*].children[*].questionCount", hasItem(2)));
  }

  @Test
  void 관리자만_쓸_수_있다() throws Exception {
    AuthUser normal = createUser("user@test.com", AppUser.Role.USER);
    mockMvc.perform(get("/api/admin/questions")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/admin/questions").with(user(normal)))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/admin/questions/1")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/admin/questions/1").with(user(normal)))
        .andExpect(status().isForbidden());

    long id = idOf(create(body(categoryId("CS", "보안"), "권한 확인용 질문입니다", "")));
    String json = body(categoryId("CS", "보안"), "권한 확인용 질문입니다", "");
    mockMvc
        .perform(
            post("/api/admin/questions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post("/api/admin/questions")
                .with(csrf())
                .with(user(normal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            put("/api/admin/questions/" + id)
                .with(csrf())
                .with(user(normal))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            patch("/api/admin/questions/" + id + "/active")
                .with(csrf())
                .with(user(normal))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"active\":false}"))
        .andExpect(status().isForbidden());
  }

  private ResultActions setActive(long id, boolean active) throws Exception {
    return mockMvc.perform(
        patch("/api/admin/questions/" + id + "/active")
            .with(csrf())
            .with(user(admin))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"active\":" + active + "}"));
  }

  @Test
  void 없는_질문의_수정_숨김은_404() throws Exception {
    mockMvc
        .perform(
            put("/api/admin/questions/999999")
                .with(csrf())
                .with(user(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(categoryId("CS", "보안"), "없는 질문을 고치려는 요청", "")))
        .andExpect(status().isNotFound());
    setActive(999999, false).andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/admin/questions/999999").with(user(admin)))
        .andExpect(status().isNotFound());
  }

  @Test
  void 질문_하나를_수정_화면용으로_불러온다() throws Exception {
    long jpa = tag("JPA");
    long id =
        idOf(create(body(categoryId("Framework", "Spring"), "지연 로딩이 동작하는 원리를 설명해 주세요.", "" + jpa)));
    mockMvc
        .perform(get("/api/admin/questions/" + id).with(user(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.categoryName").value("Spring"))
        .andExpect(jsonPath("$.tags[0].name").value("JPA"))
        .andExpect(jsonPath("$.intent").value("의도"));
  }

  @Test
  void 숨긴_질문을_다시_보이게_한다() throws Exception {
    long id = idOf(create(body(categoryId("CS", "보안"), "다시 보일 질문입니다", "")));
    setActive(id, false).andExpect(jsonPath("$.active").value(false));
    setActive(id, true).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
    mockMvc
        .perform(get("/api/admin/questions").param("q", "다시 보일 질문").with(user(admin)))
        .andExpect(jsonPath("$.total").value(1));
  }

  @Test
  void 다른_숨긴_카테고리로_옮기면_409() throws Exception {
    long id = idOf(create(body(categoryId("CS", "보안"), "옮기려는 질문입니다", "")));
    long hidden = categoryId("CS", "네트워크");
    jdbc.update("UPDATE qb_category SET active = FALSE WHERE id = ?", hidden);
    mockMvc
        .perform(
            put("/api/admin/questions/" + id)
                .with(csrf())
                .with(user(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(hidden, "옮기려는 질문입니다", "")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CATEGORY_HIDDEN"));
  }

  @Test
  void 직무_태그에_null이_섞이면_400() throws Exception {
    long cat = categoryId("CS", "보안");
    create(
            """
            {"categoryId": %d, "text": "null 직무가 섞인 질문", "level": "NEW", "jobRoles": [null], "tagIds": []}
            """
                .formatted(cat))
        .andExpect(status().isBadRequest());
    create(
            """
            {"categoryId": %d, "text": "null 태그가 섞인 질문", "level": "NEW", "jobRoles": ["BACKEND"], "tagIds": [null]}
            """
                .formatted(cat))
        .andExpect(status().isBadRequest());
  }
}
