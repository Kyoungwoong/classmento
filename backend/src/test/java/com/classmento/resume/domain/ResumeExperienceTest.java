package com.classmento.resume.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classmento.common.domain.DomainException;
import com.classmento.resume.domain.ResumeExperience.Type;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** 경험(경력·프로젝트 하나) 규칙: 제목 필수, 요약 120자, 기술 중복 제거·최대 15개. */
class ResumeExperienceTest {

  private static ResumeExperience project(
      String title, String summary, String detail, List<String> skills) {
    return ResumeExperience.create(0, Type.PROJECT, title, null, "2023", summary, detail, skills);
  }

  @Test
  void 제목이_비면_만들_수_없다() {
    assertThatThrownBy(() -> project("  ", "요약", "본문", List.of()))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).code())
        .isEqualTo("INVALID_EXPERIENCE");
  }

  @Test
  void 제목은_앞뒤_공백을_지우고_60자를_넘으면_말줄임한다() {
    ResumeExperience e = project("  " + "가".repeat(80) + "  ", "요약", "본문", List.of());
    assertThat(e.title()).hasSize(60).endsWith("…");
    assertThat(project(" 재고 동기화 ", "요약", "본문", List.of()).title()).isEqualTo("재고 동기화");
  }

  @Test
  void 요약이_없으면_본문_앞부분으로_만든다() {
    ResumeExperience e = project("제목", null, "Kafka로 여러 채널 재고를 맞췄습니다.", List.of());
    assertThat(e.summary()).isEqualTo("Kafka로 여러 채널 재고를 맞췄습니다.");
  }

  @Test
  void 요약은_120자를_넘으면_말줄임한다() {
    ResumeExperience e = project("제목", "나".repeat(200), "본문", List.of());
    assertThat(e.summary()).hasSize(120).endsWith("…");
  }

  @Test
  void 요약도_본문도_없으면_빈_문자열() {
    assertThat(project("제목", " ", null, List.of()).summary()).isEmpty();
  }

  @Test
  void 기술은_공백을_지우고_대소문자_무시_중복을_없앤다() {
    ResumeExperience e =
        project("제목", "요약", "본문", List.of(" Java", "java", "", "Spring Boot", "JAVA"));
    assertThat(e.skills()).containsExactly("Java", "Spring Boot");
  }

  @Test
  void 기술은_최대_15개() {
    List<String> many = IntStream.range(0, 20).mapToObj(i -> "skill" + i).toList();
    assertThat(project("제목", "요약", "본문", many).skills()).hasSize(15);
  }

  @Test
  void 기술_목록이_null이면_빈_목록() {
    assertThat(project("제목", "요약", "본문", null).skills()).isEmpty();
  }
}
