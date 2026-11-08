package com.classmento.resume.domain;

import com.classmento.resume.domain.ParsedResume.Experience;
import com.classmento.resume.domain.ParsedResume.Project;
import com.classmento.resume.domain.ResumeExperience.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 도메인 서비스: 구조화된 이력서(ParsedResume) → 경험(ResumeExperience) 목록.
 *
 * <ul>
 *   <li>경력(WORK) 먼저, 그다음 프로젝트(PROJECT). seq는 0부터 연속.
 *   <li>경력: 제목 "회사 · 역할", 본문 = 하이라이트 줄바꿈 연결, 기술 = 이력서 전체 기술 중 요약·본문에 나온 것.
 *   <li>프로젝트: 제목 = 이름, 본문 = 설명, 기술 = techStack.
 *   <li>제목을 만들 수 없는 항목은 건너뛴다.
 * </ul>
 */
public class ExperienceFactory {

  public List<ResumeExperience> from(ParsedResume parsed) {
    List<ResumeExperience> out = new ArrayList<>();
    for (Experience work : parsed.experiences()) {
      String title = joinNonBlank(" · ", work.company(), work.role());
      if (title.isEmpty()) {
        continue;
      }
      String detail = String.join("\n", work.highlights());
      List<String> skills = mentionedSkills(parsed.skills(), work.summary(), detail);
      out.add(
          ResumeExperience.create(
              out.size(),
              Type.WORK,
              title,
              blankToNull(work.company()),
              work.period(),
              work.summary(),
              detail,
              skills));
    }
    for (Project project : parsed.projects()) {
      if (project.name() == null || project.name().isBlank()) {
        continue;
      }
      out.add(
          ResumeExperience.create(
              out.size(),
              Type.PROJECT,
              project.name(),
              null,
              project.period(),
              project.summary(),
              project.description(),
              project.techStack()));
    }
    return out;
  }

  /** 이력서 전체 기술 중 텍스트에 (대소문자 무시) 등장하는 것만, 원래 순서대로. */
  private static List<String> mentionedSkills(List<String> allSkills, String... texts) {
    String haystack =
        Stream.of(texts)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(" "))
            .toLowerCase(Locale.ROOT);
    return allSkills.stream()
        .filter(s -> s != null && !s.isBlank())
        .filter(s -> haystack.contains(s.strip().toLowerCase(Locale.ROOT)))
        .toList();
  }

  private static String joinNonBlank(String sep, String... parts) {
    return Stream.of(parts)
        .filter(p -> p != null && !p.isBlank())
        .map(String::strip)
        .collect(Collectors.joining(sep));
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() ? null : s.strip();
  }
}
