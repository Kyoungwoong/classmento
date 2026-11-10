package com.classmento.interview.presentation;

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

/**
 * 꼬리질문 API 계약(F3): 만들 때 followUps, 질문 뷰의 사슬 정보·상태, 평가만 다시 하면 되는 실패(retryable), 리포트 사슬. ADR 0008.
 */
class InterviewFollowUpApiTest extends IntegrationTest {

  private final JsonMapper json = JsonMapper.builder().build();
  @MockitoSpyBean LlmClient llm;
  private AuthUser me;

  @BeforeEach
  void setUp() {
    me = createUser("me@test.com", AppUser.Role.USER);
  }

  private ResultActions create(String body) throws Exception {
    return mockMvc.perform(
        post("/api/interviews")
            .with(csrf())
            .with(user(me))
            .contentType(MediaType.APPLICATION_JSON)
            .content(body));
  }

  private long techWithFollowUps(int n) throws Exception {
    String res =
        create(
                "{\"type\":\"TECH\",\"count\":3,\"level\":\"JUNIOR\",\"jobRole\":\"BACKEND\",\"followUps\":"
                    + n
                    + "}")
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(res).get("id").asLong();
  }

  private ResultActions answer(long id, int seq) throws Exception {
    return mockMvc
        .perform(
            multipart("/api/interviews/" + id + "/questions/" + seq + "/answer")
                .file(new MockMultipartFile("audio", "a.webm", "audio/webm", "webm".getBytes()))
                .with(csrf())
                .with(user(me)))
        .andExpect(status().isAccepted());
  }

  @Test
  void 꼬리질문_옵션으로_면접을_만들고_질문마다_꼬리_상태를_보여준다() throws Exception {
    long id = techWithFollowUps(1);
    mockMvc
        .perform(get("/api/interviews/" + id).with(user(me)))
        .andExpect(jsonPath("$.maxFollowUps").value(1))
        .andExpect(jsonPath("$.questions[0].depth").value(0))
        .andExpect(jsonPath("$.questions[0].followUp").value("AWAITING"))
        .andExpect(jsonPath("$.questions[0].parentSeq").doesNotExist())
        .andExpect(jsonPath("$.questions[0].retryable").value(false));
  }

  @Test
  void 옵션을_빼면_꼬리_없이_만든다() throws Exception {
    create("{\"type\":\"TECH\",\"count\":3,\"level\":\"JUNIOR\",\"jobRole\":\"BACKEND\"}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.maxFollowUps").value(0))
        .andExpect(jsonPath("$.questions[0].followUp").value("NONE"));
  }

  @Test
  void 인성_면접이나_범위_밖_옵션은_거절한다() throws Exception {
    create("{\"type\":\"BEHAVIORAL\",\"count\":3,\"followUps\":1}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("FOLLOW_UPS_NOT_ALLOWED"));
    create(
            "{\"type\":\"TECH\",\"count\":3,\"level\":\"JUNIOR\",\"jobRole\":\"BACKEND\",\"followUps\":3}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_FOLLOW_UPS"));
    create(
            "{\"type\":\"TECH\",\"count\":3,\"level\":\"JUNIOR\",\"jobRole\":\"BACKEND\",\"followUps\":-1}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("INVALID_FOLLOW_UPS"));
  }

  @Test
  void 답하면_꼬리질문이_부모_바로_뒤에_사슬_정보와_함께_나온다() throws Exception {
    long id = techWithFollowUps(1);
    answer(id, 1);
    mockMvc
        .perform(get("/api/interviews/" + id).with(user(me)))
        .andExpect(jsonPath("$.questions.length()").value(4))
        .andExpect(jsonPath("$.questions[0].followUp").value("ASKED"))
        .andExpect(jsonPath("$.questions[1].seq").value(4))
        .andExpect(jsonPath("$.questions[1].parentSeq").value(1))
        .andExpect(jsonPath("$.questions[1].depth").value(1))
        .andExpect(jsonPath("$.questions[1].followUp").value("NONE"));
    mockMvc
        .perform(get("/api/interviews").with(user(me)))
        .andExpect(jsonPath("$[0].questionCount").value(3)); // 고른 본 질문 수
  }

  @Test
  void 글자는_있고_평가만_실패했으면_다시_평가하면_되는_실패로_알린다() throws Exception {
    doThrow(new LlmUnavailableException("평가 API 오류"))
        .when(llm)
        .generate(argThat(r -> r != null && r.promptName().startsWith("answer-evaluation")));
    long id = techWithFollowUps(0);
    answer(id, 1);
    mockMvc
        .perform(get("/api/interviews/" + id).with(user(me)))
        .andExpect(jsonPath("$.questions[0].answerStatus").value("FAILED"))
        .andExpect(jsonPath("$.questions[0].retryable").value(true));
  }

  @Test
  void 리포트_문항에도_사슬_정보가_있다() throws Exception {
    long id = techWithFollowUps(1);
    answer(id, 1);
    mockMvc
        .perform(get("/api/interviews/" + id + "/report").with(user(me)))
        .andExpect(jsonPath("$.items[1].seq").value(4))
        .andExpect(jsonPath("$.items[1].parentSeq").value(1))
        .andExpect(jsonPath("$.items[1].depth").value(1))
        .andExpect(jsonPath("$.items[0].depth").value(0));
  }
}
