package com.classmento.interview.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classmento.common.security.AuthUser;
import com.classmento.identity.domain.AppUser;
import com.classmento.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

/** 답변 올리기 → STT → 평가(테스트: 동기 실행, 가짜 STT·LLM), 다시 녹음, 녹음 듣기, 권한·검증. */
class InterviewAnswerApiTest extends IntegrationTest {

  private static final byte[] AUDIO = "webm-bytes".getBytes();
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
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return json.readTree(res).get("id").asLong();
  }

  private long techInterview() throws Exception {
    return interview(
        "{\"type\":\"TECH\",\"count\":3,\"level\":\"JUNIOR\",\"jobRole\":\"BACKEND\"}");
  }

  private ResultActions answer(long id, int seq, byte[] bytes, String type, AuthUser as)
      throws Exception {
    return mockMvc.perform(
        multipart("/api/interviews/" + id + "/questions/" + seq + "/answer")
            .file(new MockMultipartFile("audio", "answer.webm", type, bytes))
            .with(csrf())
            .with(user(as)));
  }

  private ResultActions answer(long id, int seq) throws Exception {
    return answer(id, seq, AUDIO, "audio/webm;codecs=opus", me);
  }

  @Test
  void 답변을_올리면_글자로_바꾸고_평가까지_한다() throws Exception {
    long id = techInterview();

    answer(id, 1).andExpect(status().isAccepted()).andExpect(jsonPath("$.seq").value(1));

    mockMvc
        .perform(get("/api/interviews/" + id).with(user(me)))
        .andExpect(jsonPath("$.questions[0].answerStatus").value("EVALUATED"))
        .andExpect(jsonPath("$.questions[1].answerStatus").doesNotExist());
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM interview_answer_score s JOIN interview_answer a"
                    + " ON a.id = s.answer_id WHERE a.session_id = ?",
                Integer.class,
                id))
        .isEqualTo(4); // 기술 루브릭 4기준
    assertThat(
            jdbc.queryForObject(
                "SELECT transcript FROM interview_answer WHERE session_id = ?", String.class, id))
        .isNotBlank();
  }

  @Test
  void 혼합은_답변마다_글자로만_바꾸고_평가는_끝낸_뒤에() throws Exception {
    long id =
        interview("{\"type\":\"MIXED\",\"count\":3,\"level\":\"MIDDLE\",\"jobRole\":\"BACKEND\"}");
    answer(id, 1).andExpect(status().isAccepted());
    mockMvc
        .perform(get("/api/interviews/" + id).with(user(me)))
        .andExpect(jsonPath("$.questions[0].answerStatus").value("TRANSCRIBED"));
  }

  @Test
  void 다시_녹음하면_같은_답변을_고친다() throws Exception {
    long id = techInterview();
    answer(id, 2).andExpect(status().isAccepted());
    answer(id, 2, "second".getBytes(), "audio/ogg", me).andExpect(status().isAccepted());

    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM interview_answer WHERE session_id = ?", Integer.class, id))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT audio_version FROM interview_answer WHERE session_id = ?",
                Integer.class,
                id))
        .isEqualTo(2);

    mockMvc
        .perform(get("/api/interviews/" + id + "/questions/2/audio").with(user(me)))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "audio/ogg"))
        .andExpect(content().bytes("second".getBytes()));
  }

  @Test
  void 녹음은_본인만_듣고_남의_면접에는_답할_수_없다() throws Exception {
    long id = techInterview();
    answer(id, 1);
    AuthUser other = createUser("other@test.com", AppUser.Role.USER);

    mockMvc
        .perform(get("/api/interviews/" + id + "/questions/1/audio").with(user(other)))
        .andExpect(status().isNotFound());
    answer(id, 1, AUDIO, "audio/webm", other).andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/interviews/" + id + "/questions/2/audio").with(user(me)))
        .andExpect(status().isNotFound()); // 아직 답하지 않은 질문
  }

  @Test
  void 오디오가_아니거나_비었거나_없는_질문이면_거절한다() throws Exception {
    long id = techInterview();
    answer(id, 1, AUDIO, "text/plain", me).andExpect(status().isBadRequest());
    answer(id, 1, new byte[0], "audio/webm", me).andExpect(status().isBadRequest());
    answer(id, 9).andExpect(status().isNotFound());
  }
}
