package com.classmento.resume.domain;

import com.classmento.common.domain.DomainException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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

  public static final int TITLE_MAX = 60;
  public static final int SUMMARY_MAX = 120;
  public static final int SKILLS_MAX = 15;

  public enum Type {
    WORK,
    PROJECT
  }

  public ResumeExperience {
    title = title == null ? "" : title.strip();
    if (title.isEmpty()) {
      throw new DomainException("INVALID_EXPERIENCE", "경험 제목이 비어 있어요.");
    }
    title = ellipsize(title, TITLE_MAX);
    summary = ellipsize(firstNonBlank(summary, detail), SUMMARY_MAX);
    skills = normalizeSkills(skills);
  }

  private static String firstNonBlank(String preferred, String fallback) {
    if (preferred != null && !preferred.isBlank()) {
      return preferred.strip();
    }
    return fallback == null ? "" : fallback.strip().replaceAll("\\s+", " ");
  }

  /** max자를 넘으면 (max-1)자 + "…"로 자른다. */
  private static String ellipsize(String s, int max) {
    return s.length() <= max ? s : s.substring(0, max - 1) + "…";
  }

  /** 앞뒤 공백 제거, 빈 값 제외, 대소문자 무시 중복 제거(처음 표기 유지), 최대 SKILLS_MAX개. */
  private static List<String> normalizeSkills(List<String> raw) {
    if (raw == null) {
      return List.of();
    }
    Set<String> seen = new HashSet<>();
    List<String> out = new ArrayList<>();
    for (String s : raw) {
      if (s == null || s.isBlank()) {
        continue;
      }
      String skill = s.strip();
      if (seen.add(skill.toLowerCase(Locale.ROOT))) {
        out.add(skill);
      }
      if (out.size() == SKILLS_MAX) {
        break;
      }
    }
    return List.copyOf(out);
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
