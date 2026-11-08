package com.classmento.support;

import com.classmento.auth.AppUser;
import com.classmento.auth.AppUserRepository;
import com.classmento.auth.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 실제 Postgres(classmento_test, localhost:5433)를 쓰는 통합 테스트 기반. DB가 없으면 `bash scripts/db.sh up`. 각
 * 테스트 전에 도메인 테이블을 비운다. AI는 test 프로필에서 가짜 구현(classmento.ai.fake=true)이 쓰인다.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTest {

  @Autowired protected MockMvc mockMvc;
  @Autowired protected JdbcTemplate jdbc;
  @Autowired protected AppUserRepository users;
  @Autowired protected PasswordEncoder passwordEncoder;

  @BeforeEach
  void cleanDatabase() {
    jdbc.execute(
        "TRUNCATE resume_chunk, resume, app_user, spring_session_attributes, spring_session"
            + " RESTART IDENTITY CASCADE");
  }

  protected AuthUser createUser(String email, AppUser.Role role) {
    AppUser user =
        users.save(AppUser.local(email, passwordEncoder.encode("password123"), "테스터", role));
    return AuthUser.from(user);
  }
}
