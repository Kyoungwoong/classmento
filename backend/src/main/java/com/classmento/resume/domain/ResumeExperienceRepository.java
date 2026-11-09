package com.classmento.resume.domain;

import java.util.List;
import java.util.Optional;

/** 경험 저장소 (도메인 포트). 구현: infrastructure.JdbcResumeExperienceRepository (pgvector). */
public interface ResumeExperienceRepository {

  /** 해당 이력서의 경험을 모두 지우고 새로 저장한다. experiences와 embeddings는 같은 순서·길이. */
  void replaceAll(Long resumeId, List<ResumeExperience> experiences, List<float[]> embeddings);

  /** seq 순서대로. */
  List<ResumeExperience> findByResumeId(Long resumeId);

  /** 이 이력서의 경험 하나의 임베딩(다른 이력서의 경험이면 빈 값). */
  Optional<float[]> embeddingOf(Long resumeId, Long experienceId);

  /** 질의 벡터와 가까운 경험 k개(코사인 거리 오름차순). */
  List<ResumeExperience> findNearest(Long resumeId, float[] query, int k);
}
