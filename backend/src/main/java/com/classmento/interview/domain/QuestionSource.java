package com.classmento.interview.domain;

import java.util.List;
import java.util.Set;

/** 질문 은행에서 면접 질문 고르기 (도메인 포트). 구현: infrastructure.QuestionBankAdapter. */
public interface QuestionSource {

  /**
   * 모자라면 있는 만큼만 돌려준다(부족 판단은 {@link InterviewSession#start}).
   *
   * @param prefer 먼저 고를 이름(이력서 기술)
   */
  List<QuestionDraft> pick(
      QuestionKind kind, InterviewLevel level, JobRole jobRole, Set<String> prefer, int count);

  /** 인성이 아닌 질문 중 벡터와 가까운 k개(경험 기반 면접의 후보, TECH). */
  List<QuestionDraft> nearestTech(float[] query, int k);
}
