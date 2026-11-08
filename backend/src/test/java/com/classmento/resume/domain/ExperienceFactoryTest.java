package com.classmento.resume.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.classmento.resume.domain.ParsedResume.Experience;
import com.classmento.resume.domain.ParsedResume.Project;
import com.classmento.resume.domain.ResumeExperience.Type;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 구조화된 이력서(ParsedResume) → 경험 목록. 경력 먼저, 그다음 프로젝트. */
class ExperienceFactoryTest {

  private final ExperienceFactory factory = new ExperienceFactory();

  private static ParsedResume resume(
      List<Experience> work, List<Project> projects, List<String> skills) {
    return new ParsedResume(null, work, skills, projects, null);
  }

  @Test
  void 경력_다음_프로젝트_순서로_번호를_매긴다() {
    List<ResumeExperience> result =
        factory.from(
            resume(
                List.of(new Experience("예시커머스", "백엔드", "2022", "요약", List.of("주문 API 개선"))),
                List.of(new Project("재고 동기화", "2023", "요약", "설명", List.of("Kafka"))),
                List.of()));

    assertThat(result).extracting(ResumeExperience::type).containsExactly(Type.WORK, Type.PROJECT);
    assertThat(result).extracting(ResumeExperience::seq).containsExactly(0, 1);
  }

  @Test
  void 경력_제목은_회사_역할이고_본문은_하이라이트를_줄바꿈으로_잇는다() {
    ResumeExperience work =
        factory
            .from(
                resume(
                    List.of(
                        new Experience(
                            "예시커머스", "백엔드 개발자", "2022.03 - 현재", null, List.of("A 개선", "B 분리"))),
                    List.of(),
                    List.of()))
            .get(0);

    assertThat(work.title()).isEqualTo("예시커머스 · 백엔드 개발자");
    assertThat(work.organization()).isEqualTo("예시커머스");
    assertThat(work.period()).isEqualTo("2022.03 - 현재");
    assertThat(work.detail()).isEqualTo("A 개선\nB 분리");
    assertThat(work.summary()).isEqualTo("A 개선 B 분리"); // 요약이 없으면 본문 앞부분
  }

  @Test
  void 회사나_역할_하나만_있으면_그것이_제목() {
    ResumeExperience work =
        factory
            .from(
                resume(
                    List.of(new Experience(null, "인턴", null, null, List.of())),
                    List.of(),
                    List.of()))
            .get(0);
    assertThat(work.title()).isEqualTo("인턴");
  }

  @Test
  void 경력에는_전체_기술_중_본문에_나온_것만_연결한다() {
    ResumeExperience work =
        factory
            .from(
                resume(
                    List.of(
                        new Experience(
                            "회사", "개발자", null, "redis로 캐시 도입", List.of("주문 API에 Spring Boot 적용"))),
                    List.of(),
                    List.of("Java", "Spring Boot", "Redis", "Kafka")))
            .get(0);

    assertThat(work.skills()).containsExactly("Spring Boot", "Redis");
  }

  @Test
  void 프로젝트는_이름이_제목이고_설명이_본문이며_기술은_techStack() {
    ResumeExperience project =
        factory
            .from(
                resume(
                    List.of(),
                    List.of(
                        new Project("재고 동기화", "2023", "요약문", "상세 설명", List.of("Kafka", "Spring"))),
                    List.of()))
            .get(0);

    assertThat(project.title()).isEqualTo("재고 동기화");
    assertThat(project.organization()).isNull();
    assertThat(project.summary()).isEqualTo("요약문");
    assertThat(project.detail()).isEqualTo("상세 설명");
    assertThat(project.skills()).containsExactly("Kafka", "Spring");
  }

  @Test
  void 제목을_만들_수_없는_항목은_건너뛴다() {
    List<ResumeExperience> result =
        factory.from(
            resume(
                List.of(new Experience(" ", null, "2020", "요약", List.of("x"))),
                List.of(
                    new Project(null, "2021", "요약", "설명", List.of()),
                    new Project("유효", null, null, null, null)),
                List.of()));

    assertThat(result).extracting(ResumeExperience::title).containsExactly("유효");
    assertThat(result.get(0).seq()).isZero();
  }

  @Test
  void 빈_이력서는_빈_목록() {
    assertThat(factory.from(new ParsedResume(null, null, null, null, null))).isEmpty();
  }
}
