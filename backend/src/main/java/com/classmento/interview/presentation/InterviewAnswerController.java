package com.classmento.interview.presentation;

import com.classmento.common.security.AuthUser;
import com.classmento.interview.application.AnswerService;
import com.classmento.interview.application.AnswerService.Audio;
import com.classmento.interview.presentation.InterviewDtos.AnswerStatusView;
import io.swagger.v3.oas.annotations.Operation;
import java.io.IOException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 면접 답변(녹음) 올리기·다시 시도·듣기. STT·평가는 백그라운드라 202로 바로 돌려준다(진행은 GET /api/interviews/{id}). */
@RestController
@RequestMapping("/api/interviews/{id}/questions/{seq}")
public class InterviewAnswerController {

  private final AnswerService answers;

  public InterviewAnswerController(AnswerService answers) {
    this.answers = answers;
  }

  @PostMapping(value = "/answer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(operationId = "uploadInterviewAnswer")
  public AnswerStatusView upload(
      @PathVariable Long id,
      @PathVariable int seq,
      @RequestPart("audio") MultipartFile audio,
      @AuthenticationPrincipal AuthUser user)
      throws IOException {
    return AnswerStatusView.of(
        answers.upload(user.id(), id, seq, audio.getBytes(), audio.getContentType()));
  }

  @PostMapping("/retry")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(operationId = "retryInterviewAnswer")
  public AnswerStatusView retry(
      @PathVariable Long id, @PathVariable int seq, @AuthenticationPrincipal AuthUser user) {
    return AnswerStatusView.of(answers.retry(user.id(), id, seq));
  }

  @GetMapping("/audio")
  @Operation(operationId = "getInterviewAnswerAudio")
  public ResponseEntity<Resource> audio(
      @PathVariable Long id, @PathVariable int seq, @AuthenticationPrincipal AuthUser user) {
    Audio a = answers.audio(user.id(), id, seq);
    // Resource로 돌려주면 Spring이 Range 요청(206)을 처리한다 — Safari <audio>는 Range가 있어야 재생·탐색한다
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(a.contentType()))
        .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
        .body(new ByteArrayResource(a.bytes()));
  }
}
