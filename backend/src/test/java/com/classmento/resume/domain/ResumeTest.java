package com.classmento.resume.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classmento.common.domain.DomainException;
import org.junit.jupiter.api.Test;

/** 이력서 처리 상태 전이: UPLOADED → EXTRACTED → PARSED, 처리 중 실패 시 FAILED. */
class ResumeTest {

  private static Resume uploaded() {
    return new Resume(1L, "cv.pdf", "application/pdf", 100, "resumes/1/a.pdf");
  }

  private static Resume parsed() {
    Resume r = uploaded();
    r.markExtracted("본문");
    r.markParsed("{}", "model", "resume-parse.v1");
    return r;
  }

  @Test
  void 정상_흐름은_업로드_추출_파싱_순서다() {
    Resume r = uploaded();
    assertThat(r.getStatus()).isEqualTo(Resume.Status.UPLOADED);

    r.markExtracted("본문");
    assertThat(r.getStatus()).isEqualTo(Resume.Status.EXTRACTED);

    r.markParsed("{}", "model", "resume-parse.v1");
    assertThat(r.getStatus()).isEqualTo(Resume.Status.PARSED);
  }

  @Test
  void 추출_전에는_파싱_완료로_바꿀_수_없다() {
    assertThatThrownBy(() -> uploaded().markParsed("{}", "m", "v1"))
        .isInstanceOf(DomainException.class)
        .extracting(e -> ((DomainException) e).code())
        .isEqualTo("RESUME_INVALID_STATE");
  }

  @Test
  void 파싱이_끝난_이력서는_다시_추출_상태로_돌아가지_않는다() {
    assertThatThrownBy(() -> parsed().markExtracted("다시")).isInstanceOf(DomainException.class);
  }

  @Test
  void 처리_중에는_실패로_바꿀_수_있고_사유를_남긴다() {
    Resume r = uploaded();
    r.markFailed("읽을 수 없어요");
    assertThat(r.getStatus()).isEqualTo(Resume.Status.FAILED);
    assertThat(r.getFailureReason()).isEqualTo("읽을 수 없어요");
  }

  @Test
  void 파싱이_끝난_이력서는_실패로_바꿀_수_없다() {
    assertThatThrownBy(() -> parsed().markFailed("늦은 실패")).isInstanceOf(DomainException.class);
  }

  @Test
  void 파싱이_끝나기_전에는_결과를_수정할_수_없다() {
    assertThatThrownBy(() -> uploaded().updateParsed("{}"))
        .isInstanceOf(DomainException.class)
        .hasMessage("아직 이력서를 읽고 있어요. 잠시 후 다시 시도해 주세요.")
        .extracting(e -> ((DomainException) e).code())
        .isEqualTo("RESUME_NOT_READY");
  }

  @Test
  void 파싱이_끝난_뒤에는_결과를_수정할_수_있다() {
    Resume r = parsed();
    r.updateParsed("{\"skills\":[\"Java\"]}");
    assertThat(r.getParsedJson()).contains("Java");
  }
}
