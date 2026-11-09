package com.classmento.questionbank.presentation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/** 관리자 태그 API(Notion식): 없으면 만들고 있으면 기존 것을 돌려준다, 자동완성은 앞부분 일치 우선. */
class TagAdminApiTest extends IntegrationTest {

  private AuthUser admin;

  @BeforeEach
  void setUp() {
    admin = createUser("admin@test.com", AppUser.Role.ADMIN);
  }

  private ResultActions create(String name) throws Exception {
    return mockMvc.perform(
        post("/api/admin/tags")
            .with(csrf())
            .with(user(admin))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"" + name + "\"}"));
  }

  @Test
  void 새_태그는_201_같은_태그는_200으로_기존_것() throws Exception {
    create("Spring Boot")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.created").value(true));
    create(" spring   boot ")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.created").value(false))
        .andExpect(jsonPath("$.tag.name").value("Spring Boot"));
  }

  @Test
  void 자동완성은_앞부분이_같은_태그를_먼저() throws Exception {
    create("Spring Boot");
    create("JPA");
    create("Spring Security");
    create("Async Spring");

    mockMvc
        .perform(get("/api/admin/tags").param("query", "spr").with(user(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].name").value("Spring Boot"))
        .andExpect(jsonPath("$[1].name").value("Spring Security"))
        .andExpect(jsonPath("$[2].name").value("Async Spring"));
  }

  @Test
  void 검색어의_퍼센트_밑줄은_글자_그대로() throws Exception {
    create("JPA");
    mockMvc
        .perform(get("/api/admin/tags").param("query", "%").with(user(admin)))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void 쉼표가_있거나_20자를_넘으면_400() throws Exception {
    create("a,b").andExpect(status().isBadRequest());
    create("a".repeat(21)).andExpect(status().isBadRequest());
  }

  @Test
  void 일반_사용자는_403() throws Exception {
    AuthUser normal = createUser("user@test.com", AppUser.Role.USER);
    mockMvc.perform(get("/api/admin/tags").with(user(normal))).andExpect(status().isForbidden());
  }
}
