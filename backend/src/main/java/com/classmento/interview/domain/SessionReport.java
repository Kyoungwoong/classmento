package com.classmento.interview.domain;

/** 면접 종합: 답변 평균 점수 + 강점·보완점 한 줄씩(session-report 프롬프트). */
public record SessionReport(
    double overall, String strength, String improvement, String promptVersion, String model) {}
