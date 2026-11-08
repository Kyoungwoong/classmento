package com.classmento.resume;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public final class ResumeDtos {

  private ResumeDtos() {}

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
