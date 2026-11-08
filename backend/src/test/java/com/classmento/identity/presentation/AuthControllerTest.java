package com.classmento.identity.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classmento.common.security.AuthUser;
import com.classmento.identity.domain.AppUser;
import com.classmento.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class AuthControllerTest extends IntegrationTest {

  private static final String SIGNUP =
      """
      {"email":"new@test.com","password":"password123","name":"새회원"}
      """;

  @Test
  void signupLogsInAndMeReturnsUser() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/signup")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(SIGNUP))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value("new@test.com"))
            .andExpect(jsonPath("$.role").value("USER"))
            .andReturn();

    // Spring Session은 SESSION 쿠키로 세션을 찾는다
    Cookie session = result.getResponse().getCookie("SESSION");
    assertThat(session).isNotNull();
    mockMvc
        .perform(get("/api/auth/me").cookie(session))
        .andExpect(jsonPath("$.authenticated").value(true))
        .andExpect(jsonPath("$.user.name").value("새회원"));
  }

  @Test
  void meWithoutLoginIsAnonymous() throws Exception {
    mockMvc
        .perform(get("/api/auth/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(false));
  }

  @Test
  void duplicateEmailIsConflict() throws Exception {
    createUser("new@test.com", AppUser.Role.USER);
    mockMvc
        .perform(
            post("/api/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(SIGNUP))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
  }

  @Test
  void shortPasswordIsRejectedWithKoreanMessage() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@test.com\",\"password\":\"123\",\"name\":\"가\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("비밀번호는 8자 이상이에요."));
  }

  /** 길이 상한(254)도 요청 검증에서 400으로 끝나야 한다(도메인 409까지 가지 않게). */
  @Test
  void tooLongEmailIsBadRequest() throws Exception {
    // 로컬 64자 + 도메인 207자 = 272자: Hibernate @Email(로컬 ≤64, 도메인 ≤255)은 통과하지만 도메인 상한 254 초과
    String label = "d".repeat(50);
    String email = "a".repeat(64) + "@" + String.join(".", label, label, label, label) + ".com";
    mockMvc
        .perform(
            post("/api/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"" + email + "\",\"password\":\"password123\",\"name\":\"가\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
  }

  /** 입력 형식 오류는 도메인(409)까지 가지 않고 요청 검증에서 400으로 끝나야 한다. */
  @Test
  void emailWithoutDomainDotIsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@b\",\"password\":\"password123\",\"name\":\"가\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.message").value("이메일 형식이 맞지 않아요."));
  }

  @Test
  void loginSucceedsAndFails() throws Exception {
    createUser("user@test.com", AppUser.Role.USER);
    mockMvc
        .perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@test.com\",\"password\":\"password123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("user@test.com"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"user@test.com\",\"password\":\"wrong-pass\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
  }

  @Test
  void postWithoutCsrfIsForbidden() throws Exception {
    mockMvc
        .perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(SIGNUP))
        .andExpect(status().isForbidden());
  }

  @Test
  void providersIsEmptyWithoutSocialKeys() throws Exception {
    mockMvc
        .perform(get("/api/auth/providers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.providers").isEmpty());
  }

  @Test
  void adminApiRequiresAdminRole() throws Exception {
    AuthUser normal = createUser("user@test.com", AppUser.Role.USER);
    AuthUser admin = createUser("admin@test.com", AppUser.Role.ADMIN);

    mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/admin/users").with(user(normal))).andExpect(status().isForbidden());
    mockMvc
        .perform(get("/api/admin/users").with(user(admin)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }
}
