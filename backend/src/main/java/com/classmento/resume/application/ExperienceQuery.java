package com.classmento.resume.application;

import com.classmento.common.web.ApiException;
import com.classmento.resume.domain.Resume;
import com.classmento.resume.domain.ResumeExperience;
import com.classmento.resume.domain.ResumeExperienceRepository;
import com.classmento.resume.domain.ResumeRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 이력서 경험의 공개 조회 서비스(다른 컨텍스트용, 면접이 infrastructure 어댑터로 부른다). 본인 것만, 남의 것은 404. */
@Service
public class ExperienceQuery {

  private final ResumeRepository resumes;
  private final ResumeExperienceRepository experiences;

  public ExperienceQuery(ResumeRepository resumes, ResumeExperienceRepository experiences) {
    this.resumes = resumes;
    this.experiences = experiences;
  }

  /** 이력서의 경험 하나와 그 임베딩. 남의 이력서이거나 그 이력서의 경험이 아니면 404. */
  @Transactional(readOnly = true)
  public ExperienceDetail experience(Long userId, Long resumeId, Long experienceId) {
    Resume resume = resumes.findByIdAndUserId(resumeId, userId).orElseThrow(ApiException::notFound);
    ResumeExperience e =
        experiences.findByResumeId(resume.getId()).stream()
            .filter(x -> x.id().equals(experienceId))
            .findFirst()
            .orElseThrow(ApiException::notFound);
    float[] embedding =
        experiences.embeddingOf(resume.getId(), experienceId).orElseThrow(ApiException::notFound);
    return new ExperienceDetail(
        e.id(), e.title(), e.summary(), e.detail(), List.copyOf(e.skills()), embedding);
  }

  /** 이력서의 경험에 적힌 기술들(중복 없이, 처음 나온 순서). 아직 정리되지 않은 이력서면 빈 집합. */
  @Transactional(readOnly = true)
  public Set<String> skills(Long userId, Long resumeId) {
    Resume resume = resumes.findByIdAndUserId(resumeId, userId).orElseThrow(ApiException::notFound);
    Set<String> skills = new LinkedHashSet<>();
    if (resume.getStatus() == Resume.Status.PARSED) {
      for (ResumeExperience e : experiences.findByResumeId(resume.getId())) {
        skills.addAll(e.skills());
      }
    }
    return skills;
  }

  /** 다른 컨텍스트에 넘기는 경험(도메인 타입을 내보내지 않는다). */
  public record ExperienceDetail(
      Long id,
      String title,
      String summary,
      String detail,
      List<String> skills,
      float[] embedding) {}
}
