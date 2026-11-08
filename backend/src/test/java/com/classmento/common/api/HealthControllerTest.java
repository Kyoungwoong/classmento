package com.classmento.common.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classmento.support.IntegrationTest;
import org.junit.jupiter.api.Test;

class HealthControllerTest extends IntegrationTest {

  @Test
  void reportsFakeAiModeWithoutKeys() throws Exception {
    mockMvc
        .perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ok"))
        .andExpect(jsonPath("$.fakeAi").value(true))
        .andExpect(jsonPath("$.llmAvailable").value(true))
        .andExpect(jsonPath("$.embeddingAvailable").value(true))
        .andExpect(jsonPath("$.sttAvailable").value(false));
  }

  @Test
  void exposesOpenApiContract() throws Exception {
    mockMvc
        .perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paths['/api/health']").exists())
        .andExpect(jsonPath("$.paths['/api/resumes']").exists());
  }
}
