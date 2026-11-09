package com.classmento.questionbank.presentation;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classmento.common.security.AuthUser;
import com.classmento.identity.domain.AppUser;
import com.classmento.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** 관리자 카테고리 API: 트리 조회·추가·이름 변경·숨김, 권한·검증·도메인 규칙. */
class CategoryAdminApiTest extends IntegrationTest {

  private final JsonMapper json = JsonMapper.builder().build();
  private AuthUser admin;

  @BeforeEach
  void setUp() {
    admin = createUser("admin@test.com", AppUser.Role.ADMIN);
  }

  private ResultActions create(String body) throws Exception {
    return mockMvc.perform(
        post("/api/admin/categories")
            .with(csrf())
            .with(user(admin))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private long idOf(ResultActions result) throws Exception {
    JsonNode node = json.readTree(result.andReturn().getResponse().getContentAsString());
    return node.get("id").asLong();
  }

  @Test
  void 트리는_대분류_순서대로_소분류를_담는다() throws Exception {
    mockMvc
        .perform(get("/api/admin/categories").with(user(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Language"))
        .andExpect(jsonPath("$[0].children[0].name").value("Java"))
        .andExpect(jsonPath("$[2].name").value("CS"))
        .andExpect(jsonPath("$[2].children[*].name", hasItem("네트워크")));
  }

  @Test
  void 대분류와_소분류를_추가한다() throws Exception {
    long top = idOf(create("{\"name\":\"모바일\"}").andExpect(status().isCreated()));
    create("{\"parentId\":" + top + ",\"name\":\"Android\"}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.parentId").value(top))
        .andExpect(jsonPath("$.name").value("Android"));
  }

  @Test
  void 같은_부모_아래_이름은_대소문자_공백을_무시하고_중복_금지() throws Exception {
    create("{\"name\":\" language \"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CATEGORY_DUPLICATE"));
  }

  @Test
  void 소분류_아래에는_만들_수_없다() throws Exception {
    long top = idOf(create("{\"name\":\"모바일\"}"));
    long child = idOf(create("{\"parentId\":" + top + ",\"name\":\"Android\"}"));
    create("{\"parentId\":" + child + ",\"name\":\"Compose\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CATEGORY_TOO_DEEP"));
  }

  @Test
  void 없는_부모는_404_빈_이름은_400() throws Exception {
    create("{\"parentId\":999999,\"name\":\"x\"}").andExpect(status().isNotFound());
    create("{\"name\":\" \"}").andExpect(status().isBadRequest());
  }

  @Test
  void 이름을_바꾸고_숨기고_다시_보인다() throws Exception {
    long top = idOf(create("{\"name\":\"모바일\"}"));
    mockMvc
        .perform(
            patch("/api/admin/categories/" + top)
                .with(csrf())
                .with(user(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"모바일 앱\",\"active\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("모바일 앱"))
        .andExpect(jsonPath("$.active").value(false));

    // 숨긴 대분류 아래에는 추가할 수 없다
    create("{\"parentId\":" + top + ",\"name\":\"iOS\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CATEGORY_HIDDEN"));
  }

  @Test
  void 형제와_같은_이름으로_바꿀_수_없다() throws Exception {
    long top = idOf(create("{\"name\":\"모바일\"}"));
    mockMvc
        .perform(
            patch("/api/admin/categories/" + top)
                .with(csrf())
                .with(user(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"CS\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CATEGORY_DUPLICATE"));
  }

  @Test
  void 관리자만_쓸_수_있다() throws Exception {
    AuthUser normal = createUser("user@test.com", AppUser.Role.USER);
    mockMvc.perform(get("/api/admin/categories")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/api/admin/categories").with(user(normal)))
        .andExpect(status().isForbidden());
  }

  private ResultActions patchCategory(long id, String body) throws Exception {
    return mockMvc.perform(
        patch("/api/admin/categories/" + id)
            .with(csrf())
            .with(user(admin))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  @Test
  void 없는_카테고리_수정은_404_빈_이름은_400() throws Exception {
    patchCategory(999999, "{\"name\":\"새 이름\"}").andExpect(status().isNotFound());
    long top = idOf(create("{\"name\":\"모바일\"}"));
    patchCategory(top, "{\"name\":\"   \"}").andExpect(status().isBadRequest());
  }

  @Test
  void 순서를_바꾸면_트리_순서가_바뀐다() throws Exception {
    long top = idOf(create("{\"name\":\"모바일\"}"));
    patchCategory(top, "{\"sortOrder\":-1}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sortOrder").value(-1));
    mockMvc
        .perform(get("/api/admin/categories").with(user(admin)))
        .andExpect(jsonPath("$[0].name").value("모바일"));
  }
}
