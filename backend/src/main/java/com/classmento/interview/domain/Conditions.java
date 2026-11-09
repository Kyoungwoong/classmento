package com.classmento.interview.domain;

/**
 * 면접 조건. 탭마다 필요한 것만 채운다.
 *
 * @param narrowByResume 기술 면접에서 이력서 기술과 맞는 질문을 먼저 고를지
 */
public record Conditions(
    InterviewLevel level,
    JobRole jobRole,
    Long resumeId,
    Long experienceId,
    boolean narrowByResume) {}
