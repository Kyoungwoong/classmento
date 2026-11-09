package com.classmento.interview.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
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
import com.classmento.llm.LlmResponse;
import com.classmento.speech.SttClient;
import com.classmento.speech.SttUnavailableException;
import com.classmento.speech.Transcript;
import com.classmento.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.json.JsonMapper;

/** STT 실패 → FAILED(원인 안내) → 다시 시도하면 처음부터. 들리지 않는 답변도 실패로 알린다. */
class InterviewAnswerFailureTest extends IntegrationTest {

  @MockitoSpyBean SttClient stt;

  private final JsonMapper json = JsonMapper.builder().build();
  private AuthUser me;
  private long id;

  @BeforeEach
  void setUp() throws Exception {
    me = createUser("me@test.com", AppUser.Role.USER);
    String res =
        mockMvc
            .perform(
                post("/api/interviews")
                    .with(csrf())
                    .with(user(me))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"type\":\"BEHAVIORAL\",\"count\":3}"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    id = json.readTree(res).get("id").asLong();
  }

  private void answer() throws Exception {
    mockMvc
        .perform(
            multipart("/api/interviews/" + id + "/questions/1/answer")
                .file(new MockMultipartFile("audio", "a.webm", "audio/webm", "x".getBytes()))
                .with(csrf())
                .with(user(me)))
        .andExpect(status().isAccepted());
  }

  @Test
  void STT가_실패하면_실패로_두고_다시_시도하면_평가까지_간다() throws Exception {
    doThrow(new SttUnavailableException("down")).when(stt).transcribe(any(), anyString());
    answer();
    mockMvc
        .perform(get("/api/interviews/" + id).with(user(me)))
        .andExpect(jsonPath("$.questions[0].answerStatus").value("FAILED"))
        .andExpect(jsonPath("$.questions[0].failureReason").value("답변을 글자로 바꾸지 못했어요. 다시 시도해 주세요."));

    Mockito.reset(stt);
    mockMvc
        .perform(post("/api/interviews/" + id + "/questions/1/retry").with(csrf()).with(user(me)))
        .andExpect(status().isAccepted());
    mockMvc
        .perform(get("/api/interviews/" + id).with(user(me)))
        .andExpect(jsonPath("$.questions[0].answerStatus").value("EVALUATED"));
  }

  @Test
  void 들리지_않는_답변은_다시_녹음하라고_알린다() throws Exception {
    doReturn(new Transcript("  ", 0)).when(stt).transcribe(any(), anyString());
    answer();
    mockMvc
        .perform(get("/api/interviews/" + id).with(user(me)))
        .andExpect(jsonPath("$.questions[0].answerStatus").value("FAILED"))
        .andExpect(jsonPath("$.questions[0].failureReason").value("답변이 들리지 않았어요. 다시 녹음해 주세요."));
  }

  @MockitoSpyBean LlmClient llm;

  @Test
  void 평가가_두_번_깨지면_실패로_두고_다시_시도하면_STT없이_평가만() throws Exception {
    doReturn(new LlmResponse("평가할 수 없어요", "m", 0, 0)).when(llm).generate(any());
    answer();
    mockMvc
        .perform(get("/api/interviews/" + id).with(user(me)))
        .andExpect(jsonPath("$.questions[0].answerStatus").value("FAILED"))
        .andExpect(jsonPath("$.questions[0].failureReason").value("답변을 평가하지 못했어요. 다시 시도해 주세요."));

    Mockito.reset(llm);
    Mockito.clearInvocations(stt);
    mockMvc
        .perform(post("/api/interviews/" + id + "/questions/1/retry").with(csrf()).with(user(me)))
        .andExpect(status().isAccepted());
    Mockito.verify(stt, Mockito.never()).transcribe(any(), anyString());
    mockMvc
        .perform(get("/api/interviews/" + id).with(user(me)))
        .andExpect(jsonPath("$.questions[0].answerStatus").value("EVALUATED"));
  }

  @Test
  void 처리_중_예상하지_못한_오류가_나도_실패로_남겨_다시_시도할_수_있다() throws Exception {
    doThrow(new IllegalStateException("boom")).when(llm).generate(any());
    doThrow(new IllegalStateException("db?")).when(stt).transcribe(any(), anyString());
    answer();
    mockMvc
        .perform(get("/api/interviews/" + id).with(user(me)))
        .andExpect(jsonPath("$.questions[0].answerStatus").value("FAILED"));
  }

  @Test
  void 실패하지_않은_답변은_다시_시도할_수_없다() throws Exception {
    answer();
    mockMvc
        .perform(post("/api/interviews/" + id + "/questions/1/retry").with(csrf()).with(user(me)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("NOT_FAILED"));
  }
}
