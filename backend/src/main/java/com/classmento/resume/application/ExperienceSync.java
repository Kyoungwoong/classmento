package com.classmento.resume.application;

import com.classmento.embedding.EmbeddingClient;
import com.classmento.resume.domain.ExperienceFactory;
import com.classmento.resume.domain.ParsedResume;
import com.classmento.resume.domain.ResumeExperience;
import com.classmento.resume.domain.ResumeExperienceRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 구조화 결과 → 경험 목록을 만들고 임베딩해 저장한다. 파싱 직후와 사용자가 결과를 고쳤을 때 호출한다. 임베딩 문장은 제목·요약·본문·기술을 이어 붙인 것(경험 기반
 * 면접에서 관련 질문을 찾는 기준).
 */
@Service
public class ExperienceSync {

  private final ExperienceFactory factory = new ExperienceFactory();
  private final EmbeddingClient embeddings;
  private final ResumeExperienceRepository experiences;

  public ExperienceSync(EmbeddingClient embeddings, ResumeExperienceRepository experiences) {
    this.embeddings = embeddings;
    this.experiences = experiences;
  }

  public List<ResumeExperience> sync(Long resumeId, ParsedResume parsed) {
    List<ResumeExperience> list = factory.from(parsed);
    List<float[]> vectors =
        embeddings.embedAll(list.stream().map(ExperienceSync::embeddingText).toList());
    experiences.replaceAll(resumeId, list, vectors);
    return list;
  }

  static String embeddingText(ResumeExperience e) {
    return String.join(
        "\n",
        e.title(),
        e.summary(),
        e.detail() == null ? "" : e.detail(),
        String.join(", ", e.skills()));
  }
}
