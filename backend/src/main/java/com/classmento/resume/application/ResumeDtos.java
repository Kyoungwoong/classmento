package com.classmento.resume.application;

import com.classmento.resume.domain.ParsedResume;
import com.classmento.resume.domain.Resume;
import com.classmento.resume.domain.ResumeExperience;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public final class ResumeDtos {

  private ResumeDtos() {}

  /** 경험 기반 면접에서 고르는 단위. 배너에 제목·요약·기술을 보여준다. */
  @Schema(requiredProperties = {"id", "seq", "type", "title", "summary", "skills"})
  public record ExperienceView(
      Long id,
      int seq,
      ResumeExperience.Type type,
      String title,
      String organization,
      String period,
      String summary,
      List<String> skills) {

    static ExperienceView of(ResumeExperience e) {
      return new ExperienceView(
          e.id(),
          e.seq(),
          e.type(),
          e.title(),
          e.organization(),
          e.period(),
          e.summary(),
          e.skills());
    }
  }

  @Schema(requiredProperties = {"id", "filename", "status", "createdAt"})
  public record ResumeSummary(Long id, String filename, Resume.Status status, Instant createdAt) {

    static ResumeSummary of(Resume r) {
      return new ResumeSummary(r.getId(), r.getOriginalFilename(), r.getStatus(), r.getCreatedAt());
    }
  }

  /**
   * @param parsed status가 PARSED일 때만 값이 있다
   * @param failureReason status가 FAILED일 때 사용자에게 보여줄 문장
   */
  @Schema(requiredProperties = {"id", "filename", "sizeBytes", "status", "createdAt"})
  public record ResumeDetail(
      Long id,
      String filename,
      long sizeBytes,
      Resume.Status status,
      String failureReason,
      ParsedResume parsed,
      Instant createdAt) {

    static ResumeDetail of(Resume r, ParsedResume parsed) {
      return new ResumeDetail(
          r.getId(),
          r.getOriginalFilename(),
          r.getSizeBytes(),
          r.getStatus(),
          r.getFailureReason(),
          parsed,
          r.getCreatedAt());
    }
  }
}
