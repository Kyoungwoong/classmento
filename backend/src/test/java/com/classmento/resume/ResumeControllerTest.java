package com.classmento.resume;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.classmento.auth.AppUser;
import com.classmento.common.security.AuthUser;
import com.classmento.support.IntegrationTest;
import com.classmento.support.SampleFiles;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

class ResumeControllerTest extends IntegrationTest {

  @Autowired ResumeChunkRepository chunks;

  @Test
  void uploadPdfIsExtractedEmbeddedAndParsed() throws Exception {
    AuthUser me = createUser("me@test.com", AppUser.Role.USER);
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "resume.pdf", "application/pdf", SampleFiles.pdf(SampleFiles.RESUME_TEXT));

    // 테스트 프로필은 동기 처리라 응답 시점에 이미 PARSED
    mockMvc
        .perform(multipart("/api/resumes").file(file).with(csrf()).with(user(me)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.status").value("PARSED"));

    mockMvc
        .perform(get("/api/resumes/1").with(user(me)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PARSED"))
        .andExpect(jsonPath("$.parsed.skills[0]").value("Java"));

    assertThat(chunks.count(1L)).isPositive();
  }

  @Test
  void uploadDocxWorks() throws Exception {
    AuthUser me = createUser("me@test.com", AppUser.Role.USER);
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "이력서.docx",
            ResumeFileType.DOCX.contentType(),
            SampleFiles.docx(SampleFiles.RESUME_TEXT_KO));

    mockMvc
        .perform(multipart("/api/resumes").file(file).with(csrf()).with(user(me)))
        .andExpect(status().isAccepted());
    mockMvc
        .perform(get("/api/resumes/1").with(user(me)))
        .andExpect(jsonPath("$.status").value("PARSED"))
        .andExpect(jsonPath("$.filename").value("이력서.docx"));
  }

  @Test
  void rejectsUnsupportedFile() throws Exception {
    AuthUser me = createUser("me@test.com", AppUser.Role.USER);
    MockMultipartFile file =
        new MockMultipartFile("file", "resume.txt", "text/plain", "hello".getBytes());
    mockMvc
        .perform(multipart("/api/resumes").file(file).with(csrf()).with(user(me)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("PDF나 DOCX 파일만 올릴 수 있어요."));
  }

  @Test
  void pdfDisguisedAsDocxIsRejected() throws Exception {
    AuthUser me = createUser("me@test.com", AppUser.Role.USER);
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "resume.docx", "application/octet-stream", "not a zip".getBytes());
    mockMvc
        .perform(multipart("/api/resumes").file(file).with(csrf()).with(user(me)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cannotSeeOthersResume() throws Exception {
    AuthUser owner = createUser("owner@test.com", AppUser.Role.USER);
    AuthUser other = createUser("other@test.com", AppUser.Role.USER);
    MockMultipartFile file =
        new MockMultipartFile(
            "file", "resume.pdf", "application/pdf", SampleFiles.pdf(SampleFiles.RESUME_TEXT));
    mockMvc.perform(multipart("/api/resumes").file(file).with(csrf()).with(user(owner)));

    mockMvc.perform(get("/api/resumes/1").with(user(other))).andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/resumes").with(user(other)))
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void requiresLogin() throws Exception {
    mockMvc.perform(get("/api/resumes")).andExpect(status().isUnauthorized());
  }
}
