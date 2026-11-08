package com.classmento.resume.domain;

import java.util.List;

/**
 * 이력서의 경험 하나(경력 또는 프로젝트). 경험 기반 면접에서 사용자가 고르는 단위다(ADR 0005).
 *
 * @param id 저장 전에는 null
 * @param seq 이력서 안의 순서(0부터)
 */
public record ResumeExperience(
    Long id,
    int seq,
    Type type,
    String title,
    String organization,
    String period,
    String summary,
    String detail,
    List<String> skills) {

  public enum Type {
    WORK,
    PROJECT
  }

  public static ResumeExperience create(
      int seq,
      Type type,
      String title,
      String organization,
      String period,
      String summary,
      String detail,
      List<String> skills) {
    return new ResumeExperience(
        null, seq, type, title, organization, period, summary, detail, skills);
  }
}
