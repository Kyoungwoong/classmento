package com.classmento.identity.infrastructure.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classmento.support.IntegrationTest;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * 세션 DB에 역직렬화할 수 없는 값(예: 패키지가 바뀐 옛 principal 클래스)이 남아 있어도, 그 쿠키를 가진 사용자는 500이 아니라 "로그인 안 됨"으로 처리돼
 * 다시 로그인할 수 있어야 한다. (PR #3에서 AuthUser 패키지 이동 후 실제로 발생)
 */
class StaleSessionTest extends IntegrationTest {

  private Cookie staleSessionCookie() {
    String primaryId = UUID.randomUUID().toString();
    String sessionId = UUID.randomUUID().toString();
    long now = System.currentTimeMillis();
    jdbc.update(
        "INSERT INTO spring_session (primary_id, session_id, creation_time, last_access_time,"
            + " max_inactive_interval, expiry_time, principal_name) VALUES (?, ?, ?, ?, ?, ?, ?)",
        primaryId,
        sessionId,
        now,
        now,
        3600,
        now + 3_600_000,
        "1");
    jdbc.update(
        "INSERT INTO spring_session_attributes (session_primary_id, attribute_name, attribute_bytes)"
            + " VALUES (?, ?, ?)",
        primaryId,
        "SPRING_SECURITY_CONTEXT",
        new byte[] {1, 2, 3, 4}); // 자바 직렬화 형식이 아닌 깨진 값
    String cookieValue =
        Base64.getEncoder().encodeToString(sessionId.getBytes(StandardCharsets.UTF_8));
    return new Cookie("SESSION", cookieValue);
  }

  @Test
  void 깨진_세션이어도_공개_API는_정상_응답하고_로그인_안_됨으로_본다() throws Exception {
    mockMvc
        .perform(get("/api/auth/me").cookie(staleSessionCookie()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(false));
  }

  @Test
  void 깨진_세션으로_보호된_API는_401() throws Exception {
    mockMvc
        .perform(get("/api/resumes").cookie(staleSessionCookie()))
        .andExpect(status().isUnauthorized());
  }
}
