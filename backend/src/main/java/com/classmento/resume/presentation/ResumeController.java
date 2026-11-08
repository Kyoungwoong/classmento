package com.classmento.resume.presentation;

import com.classmento.common.security.AuthUser;
import com.classmento.resume.application.ResumeDtos.ResumeDetail;
import com.classmento.resume.application.ResumeDtos.ResumeSummary;
import com.classmento.resume.application.ResumeService;
import com.classmento.resume.domain.ParsedResume;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

  private final ResumeService service;

  public ResumeController(ResumeService service) {
    this.service = service;
  }

  /** 202: 저장만 하고 바로 응답. 이후 GET /api/resumes/{id}로 status를 폴링한다. */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public ResumeDetail upload(
      @AuthenticationPrincipal AuthUser user, @RequestPart("file") MultipartFile file) {
    return service.upload(user.id(), file);
  }

  @GetMapping
  public List<ResumeSummary> list(@AuthenticationPrincipal AuthUser user) {
    return service.list(user.id());
  }

  @GetMapping("/{id}")
  public ResumeDetail detail(@AuthenticationPrincipal AuthUser user, @PathVariable Long id) {
    return service.detail(user.id(), id);
  }

  @PutMapping("/{id}/parsed")
  public ResumeDetail updateParsed(
      @AuthenticationPrincipal AuthUser user,
      @PathVariable Long id,
      @RequestBody ParsedResume parsed) {
    return service.updateParsed(user.id(), id, parsed);
  }
}
