package com.classmento.interview.infrastructure;

import com.classmento.interview.domain.Experience;
import com.classmento.interview.domain.ExperienceSource;
import com.classmento.resume.application.ExperienceQuery;
import com.classmento.resume.application.ExperienceQuery.ExperienceDetail;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 이력서 컨텍스트로 가는 어댑터. 공개 조회 서비스 {@link ExperienceQuery}만 쓴다(ArchitectureTest). */
@Component
public class ResumeAdapter implements ExperienceSource {

  private final ExperienceQuery experiences;

  public ResumeAdapter(ExperienceQuery experiences) {
    this.experiences = experiences;
  }

  @Override
  public Set<String> resumeSkills(Long userId, Long resumeId) {
    return experiences.skills(userId, resumeId);
  }

  @Override
  public Experience experience(Long userId, Long resumeId, Long experienceId) {
    ExperienceDetail d = experiences.experience(userId, resumeId, experienceId);
    return new Experience(d.title(), d.summary(), d.detail(), d.skills(), d.embedding());
  }
}
