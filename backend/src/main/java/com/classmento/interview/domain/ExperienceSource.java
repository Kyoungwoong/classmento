package com.classmento.interview.domain;

import java.util.Set;

/** 이력서 경험 조회 (도메인 포트). 구현: infrastructure.ResumeAdapter. 남의 이력서면 404. */
public interface ExperienceSource {

  /** 이력서 경험에 적힌 기술들 */
  Set<String> resumeSkills(Long userId, Long resumeId);

  /** 이력서의 경험 하나(임베딩 포함). 남의 이력서이거나 그 이력서의 경험이 아니면 404. */
  Experience experience(Long userId, Long resumeId, Long experienceId);
}
