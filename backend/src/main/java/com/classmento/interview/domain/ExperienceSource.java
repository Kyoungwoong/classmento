package com.classmento.interview.domain;

import java.util.Set;

/** 이력서 경험 조회 (도메인 포트). 구현: infrastructure.ResumeAdapter. 남의 이력서면 404. */
public interface ExperienceSource {

  /** 이력서 경험에 적힌 기술들 */
  Set<String> resumeSkills(Long userId, Long resumeId);
}
