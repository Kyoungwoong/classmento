package com.classmento.resume.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Claude가 이력서를 구조화한 결과(resume-parse 프롬프트의 출력 형식). 사용자가 확인 화면에서 수정할 수 있다. 목록 필드는 null 대신 빈 리스트로
 * 정규화한다.
 */
@Schema(requiredProperties = {"profile", "experiences", "skills", "projects", "educations"})
public record ParsedResume(
    Profile profile,
    List<Experience> experiences,
    List<String> skills,
    List<Project> projects,
    List<Education> educations) {

  public ParsedResume {
    profile = profile == null ? new Profile(null, null, null) : profile;
    experiences = experiences == null ? List.of() : experiences;
    skills = skills == null ? List.of() : skills;
    projects = projects == null ? List.of() : projects;
    educations = educations == null ? List.of() : educations;
  }

  /**
   * @param headline 한 줄 소개 (예: "백엔드 개발자")
   */
  public record Profile(String name, String headline, Integer yearsOfExperience) {}

  /**
   * @param summary 한두 문장 요약(resume-parse.v2부터). v1 데이터는 null
   */
  @Schema(requiredProperties = {"highlights"})
  public record Experience(
      String company, String role, String period, String summary, List<String> highlights) {
    public Experience {
      highlights = highlights == null ? List.of() : highlights;
    }
  }

  /**
   * @param summary 한두 문장 요약(resume-parse.v2부터). v1 데이터는 null
   */
  @Schema(requiredProperties = {"techStack"})
  public record Project(
      String name, String period, String summary, String description, List<String> techStack) {
    public Project {
      techStack = techStack == null ? List.of() : techStack;
    }
  }

  public record Education(String school, String major, String period) {}
}
