package com.classmento.resume.application;

import com.classmento.embedding.EmbeddingClient;
import com.classmento.resume.domain.ExperienceFactory;
import com.classmento.resume.domain.ParsedResume;
import com.classmento.resume.domain.ResumeExperience;
import com.classmento.resume.domain.ResumeExperienceRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 구조화 결과 → 경험 목록 + 임베딩을 만들고 저장한다. 두 단계로 나눈다.
 *
 * <ol>
 *   <li>{@link #prepare}: 경험 생성과 임베딩(외부 API). <b>트랜잭션 밖에서</b> 호출한다(DB 커넥션을 잡은 채 외부 호출하지 않도록).
 *   <li>{@link #save}: 저장만. 이력서 상태 변경과 <b>같은 트랜잭션</b>에서 호출해 원자적으로 반영한다.
 * </ol>
 *
 * 임베딩 문장은 제목·요약·본문·기술을 이어 붙인 것(경험 기반 면접에서 관련 질문을 찾는 기준).
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

  public Prepared prepare(ParsedResume parsed) {
    List<ResumeExperience> list = factory.from(parsed);
    List<float[]> vectors =
        embeddings.embedAll(list.stream().map(ExperienceSync::embeddingText).toList());
    return new Prepared(list, vectors);
  }

  public void save(Long resumeId, Prepared prepared) {
    experiences.replaceAll(resumeId, prepared.experiences(), prepared.vectors());
  }

  static String embeddingText(ResumeExperience e) {
    return String.join(
        "\n",
        e.title(),
        e.summary(),
        e.detail() == null ? "" : e.detail(),
        String.join(", ", e.skills()));
  }

  /** 저장 직전 상태(경험 목록과 같은 순서의 임베딩). */
  public record Prepared(List<ResumeExperience> experiences, List<float[]> vectors) {}
}
