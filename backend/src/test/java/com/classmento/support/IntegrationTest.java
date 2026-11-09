package com.classmento.support;

import com.classmento.common.security.AuthUser;
import com.classmento.identity.domain.AppUser;
import com.classmento.identity.domain.AppUserRepository;
import com.classmento.identity.domain.Email;
import com.classmento.identity.infrastructure.security.AuthUsers;
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
    // 질문 은행은 기본 데이터(seeded)를 남기고 테스트가 만든 것만 지운다(소분류 → 대분류 순)
    jdbc.update("DELETE FROM qb_question WHERE NOT seeded");
    jdbc.update("UPDATE qb_question SET active = TRUE WHERE seeded");
    jdbc.update("DELETE FROM qb_category WHERE NOT seeded AND parent_id IS NOT NULL");
    jdbc.update("DELETE FROM qb_category WHERE NOT seeded");
    jdbc.update("UPDATE qb_category SET active = TRUE WHERE seeded");
    jdbc.update("DELETE FROM qb_tag WHERE NOT seeded");
  }

  protected AuthUser createUser(String email, AppUser.Role role) {
    AppUser user =
        users.save(
            AppUser.local(Email.of(email), passwordEncoder.encode("password123"), "테스터", role));
    return AuthUsers.from(user);
  }
}
