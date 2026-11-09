package com.classmento.interview.presentation;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classmento.common.security.AuthUser;
import com.classmento.identity.domain.AppUser;
import com.classmento.llm.LlmClient;
import com.classmento.llm.LlmUnavailableException;
import com.classmento.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

/** 끝내기 → 종합 리포트(테스트: 동기 실행, 가짜 STT·LLM). 가짜 평가: 기술 4·3·4·4 → 3.8, 인성 3·4·2(근거 없음)·3 → 3.0. */
class InterviewReportApiTest extends IntegrationTest {

  @MockitoSpyBean LlmClient llm;

  private final JsonMapper json = JsonMapper.builder().build();
  private AuthUser me;

  @BeforeEach
  void setUp() {
    me = createUser("me@test.com", AppUser.Role.USER);
  }

  private long interview(String body) throws Exception {
    String res =
        mockMvc
            .perform(
                post("/api/interviews")
                    .with(csrf())
                    .with(user(me))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(res).get("id").asLong();
  }

  private void answerAll(long id, int count) throws Exception {
    for (int seq = 1; seq <= count; seq++) {
      mockMvc
          .perform(
              multipart("/api/interviews/" + id + "/questions/" + seq + "/answer")
                  .file(new MockMultipartFile("audio", "a.webm", "audio/webm", "x".getBytes()))
                  .with(csrf())
                  .with(user(me)))
          .andExpect(status().isAccepted());
    }
  }

  private ResultActions finish(long id, AuthUser as) throws Exception {
    return mockMvc.perform(post("/api/interviews/" + id + "/finish").with(csrf()).with(user(as)));
  }

  private ResultActions report(long id, AuthUser as) throws Exception {
    return mockMvc.perform(get("/api/interviews/" + id + "/report").with(user(as)));
  }

  @Test
  void 모두_답하고_끝내면_종합_리포트가_나온다() throws Exception {
    long id =
        interview("{\"type\":\"TECH\",\"count\":3,\"level\":\"JUNIOR\",\"jobRole\":\"BACKEND\"}");
    answerAll(id, 3);

    finish(id, me)
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("FINISHED"));

    report(id, me)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("READY"))
        .andExpect(jsonPath("$.overall").value(3.8))
        .andExpect(jsonPath("$.strength").isNotEmpty())
        .andExpect(jsonPath("$.improvement").isNotEmpty())
        .andExpect(
            jsonPath("$.criteria[*].criterion")
                .value(contains("accuracy", "depth", "problem_solving", "communication")))
        .andExpect(jsonPath("$.criteria[1].average").value(3.0))
        .andExpect(jsonPath("$.items.length()").value(3))
        .andExpect(jsonPath("$.items[0].transcript").isNotEmpty())
        .andExpect(jsonPath("$.items[0].overall").value(3.8))
        .andExpect(jsonPath("$.items[0].scores.length()").value(4))
        .andExpect(jsonPath("$.items[0].suggestion").isNotEmpty());
  }

  @Test
  void 혼합은_끝낸_뒤에_한꺼번에_평가한다() throws Exception {
    long id =
        interview("{\"type\":\"MIXED\",\"count\":3,\"level\":\"MIDDLE\",\"jobRole\":\"BACKEND\"}");
    answerAll(id, 3);
    report(id, me)
        .andExpect(jsonPath("$.status").value("EVALUATING"))
        .andExpect(jsonPath("$.items[0].answerStatus").value("TRANSCRIBED"));

    finish(id, me).andExpect(status().isAccepted());

    report(id, me)
        .andExpect(jsonPath("$.status").value("READY"))
        .andExpect(
            jsonPath("$.items[*].answerStatus")
                .value(contains("EVALUATED", "EVALUATED", "EVALUATED")))
        // 기술 3.8 두 개 + 인성 3.0 하나 → 3.5(소수 첫째 자리)
        .andExpect(jsonPath("$.overall").value(3.5))
        .andExpect(jsonPath("$.criteria.length()").value(8));
  }

  @Test
  void 다_답하지_않으면_끝낼_수_없고_끝나면_답할_수_없다() throws Exception {
    long id = interview("{\"type\":\"BEHAVIORAL\",\"count\":3}");
    answerAll(id, 2);
    finish(id, me)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("NOT_ALL_ANSWERED"));

    answerAll(id, 3);
    finish(id, me).andExpect(status().isAccepted());
    finish(id, me)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ALREADY_FINISHED"));
    mockMvc
        .perform(
            multipart("/api/interviews/" + id + "/questions/1/answer")
                .file(new MockMultipartFile("audio", "a.webm", "audio/webm", "x".getBytes()))
                .with(csrf())
                .with(user(me)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INTERVIEW_CLOSED"));
  }

  @Test
  void 종합_문장을_못_만들면_답변_피드백으로_대신한다() throws Exception {
    doThrow(new LlmUnavailableException("down"))
        .when(llm)
        .generate(argThat(r -> r != null && r.promptName().startsWith("session-report")));
    long id = interview("{\"type\":\"BEHAVIORAL\",\"count\":3}");
    answerAll(id, 3);
    finish(id, me).andExpect(status().isAccepted());
    report(id, me)
        .andExpect(jsonPath("$.status").value("READY"))
        .andExpect(jsonPath("$.strength").value(containsString("가짜 평가")));
  }

  @Test
  void 종합이_빠진_채_끝났어도_리포트를_조회하면_다시_만든다() throws Exception {
    long id = interview("{\"type\":\"BEHAVIORAL\",\"count\":3}");
    answerAll(id, 3);
    finish(id, me).andExpect(status().isAccepted());
    // 끝내기 직후 종합 작업이 거절·실패한 상황을 흉내 낸다
    jdbc.update(
        "UPDATE interview_session SET status = 'FINISHED', report_overall = NULL,"
            + " report_strength = NULL, report_improvement = NULL WHERE id = ?",
        id);

    report(id, me).andExpect(jsonPath("$.status").value("EVALUATING"));
    report(id, me)
        .andExpect(jsonPath("$.status").value("READY"))
        .andExpect(jsonPath("$.strength").isNotEmpty());
  }

  @Test
  void 리포트는_본인만() throws Exception {
    long id = interview("{\"type\":\"BEHAVIORAL\",\"count\":3}");
    AuthUser other = createUser("other@test.com", AppUser.Role.USER);
    report(id, other).andExpect(status().isNotFound());
    finish(id, other).andExpect(status().isNotFound());
    report(id, me)
        .andExpect(jsonPath("$.status").value("EVALUATING"))
        .andExpect(jsonPath("$.items[0].answerStatus").doesNotExist());
  }
}
