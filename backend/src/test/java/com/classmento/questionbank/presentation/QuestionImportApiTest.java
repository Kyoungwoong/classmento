package com.classmento.questionbank.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classmento.common.security.AuthUser;
import com.classmento.identity.domain.AppUser;
import com.classmento.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

/** CSV 대량 등록: 전부 검사한 뒤 한 번에 저장, 오류가 하나라도 있으면 0건 + 줄별 오류. */
class QuestionImportApiTest extends IntegrationTest {

  private static final String HEADER = "대분류,소분류,질문,난이도,직무,태그,의도\n";
  private AuthUser admin;

  @BeforeEach
  void setUp() {
    admin = createUser("admin@test.com", AppUser.Role.ADMIN);
  }

  private ResultActions upload(byte[] bytes, AuthUser as) throws Exception {
    return mockMvc.perform(
        multipart("/api/admin/questions/import")
            .file(new MockMultipartFile("file", "questions.csv", "text/csv", bytes))
            .with(csrf())
            .with(user(as)));
  }

  private ResultActions upload(String csv) throws Exception {
    return upload(csv.getBytes(StandardCharsets.UTF_8), admin);
  }

  private int questionCount() {
    return jdbc.queryForObject("SELECT count(*) FROM qb_question", Integer.class);
  }

  @Test
  void 정상_파일은_모두_저장하고_태그를_만들거나_재사용하고_임베딩을_채운다() throws Exception {
    jdbc.update("INSERT INTO qb_tag (name, name_key) VALUES ('JPA', 'jpa')");

    upload(
            HEADER
                + "Framework,Spring,영속성 컨텍스트를 설명해 주세요.,주니어,백엔드,jpa|영속성,\n"
                + " cs , 네트워크 ,HTTP/2의 특징을 말해 주세요.,MIDDLE,BACKEND|FRONTEND,,성능 이해\n")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imported").value(2))
        .andExpect(jsonPath("$.errors.length()").value(0));

    assertThat(questionCount()).isEqualTo(48);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM qb_tag", Integer.class)).isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM qb_question WHERE NOT seeded AND embedding IS NOT NULL",
                Integer.class))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT c.name FROM qb_question q JOIN qb_category c ON c.id = q.category_id"
                    + " WHERE q.text LIKE 'HTTP/2%'",
                String.class))
        .isEqualTo("네트워크");
  }

  @Test
  void 없는_카테고리나_숨긴_카테고리가_있으면_한_건도_저장하지_않는다() throws Exception {
    jdbc.update(
        "UPDATE qb_category SET active = FALSE WHERE name = '보안'"
            + " AND parent_id = (SELECT id FROM qb_category WHERE name = 'CS' AND parent_id IS NULL)");

    upload(
            HEADER
                + "CS,네트워크,정상인 질문이지만 같이 취소돼요.,신입,공통,,\n"
                + "CS,양자컴퓨팅,없는 소분류에 넣으려는 질문,신입,공통,,\n"
                + "없는대분류,네트워크,없는 대분류에 넣으려는 질문,신입,공통,,\n"
                + "CS,보안,숨긴 소분류에 넣으려는 질문,신입,공통,,\n"
                + "Language,네트워크,다른 대분류의 소분류 이름,신입,공통,,\n")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imported").value(0))
        .andExpect(jsonPath("$.errors[*].line").value(org.hamcrest.Matchers.contains(3, 4, 5, 6)))
        .andExpect(jsonPath("$.errors[0].message").value("없는 카테고리예요: CS > 양자컴퓨팅"))
        .andExpect(jsonPath("$.errors[2].message").value("숨긴 카테고리에는 넣을 수 없어요: CS > 보안"));

    assertThat(questionCount()).isEqualTo(46);
  }

  @Test
  void 형식_오류와_카테고리_오류를_한_번에_줄_순서대로_알려준다() throws Exception {
    upload(HEADER + "CS,양자컴퓨팅,없는 소분류에 넣으려는 질문,신입,공통,,\n" + "CS,보안,짧아,신입,공통,,\n")
        .andExpect(jsonPath("$.imported").value(0))
        .andExpect(jsonPath("$.errors[*].line").value(org.hamcrest.Matchers.contains(2, 3)));
  }

  @Test
  void 형식_오류도_줄별로_알려주고_저장하지_않는다() throws Exception {
    upload(HEADER + "CS,네트워크,짧아,신입,공통,,\n")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.imported").value(0))
        .andExpect(jsonPath("$.errors[0].line").value(2))
        .andExpect(jsonPath("$.errors[0].message").value("질문은 5~300자로 입력해 주세요."));
    assertThat(questionCount()).isEqualTo(46);
  }

  @Test
  void 파일이_2MB를_넘거나_UTF8이_아니면_400() throws Exception {
    upload(new byte[2 * 1024 * 1024 + 1], admin)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("CSV 파일은 2MB까지 올릴 수 있어요."));

    byte[] cp949 = (HEADER + "CS,네트워크,인코딩이 다른 질문입니다,신입,공통,,\n").getBytes("MS949");
    upload(cp949, admin)
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("UTF-8로 저장한 CSV를 올려 주세요. 엑셀이라면 'CSV UTF-8'로 저장해요."));
  }

  @Test
  void 파일_칸이_없으면_400() throws Exception {
    mockMvc
        .perform(multipart("/api/admin/questions/import").with(csrf()).with(user(admin)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void 관리자만_올릴_수_있다() throws Exception {
    mockMvc
        .perform(
            multipart("/api/admin/questions/import")
                .file(new MockMultipartFile("file", "q.csv", "text/csv", new byte[0]))
                .with(csrf()))
        .andExpect(status().isUnauthorized());
    AuthUser normal = createUser("user@test.com", AppUser.Role.USER);
    upload(HEADER.getBytes(StandardCharsets.UTF_8), normal).andExpect(status().isForbidden());
  }
}
