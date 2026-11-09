package com.classmento.interview.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classmento.common.domain.DomainException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** 답변: 녹음 → STT → 평가, 다시 녹음하면 처음부터. 평가: 루브릭 기준·점수 범위·근거 없으면 3점까지. */
class AnswerTest {

  private static final Conditions TECH =
      new Conditions(InterviewLevel.JUNIOR, JobRole.BACKEND, null, null, false);

  private static InterviewSession session() {
    return InterviewSession.start(
        7L,
        InterviewType.TECH,
        3,
        TECH,
        IntStream.range(0, 3)
            .mapToObj(i -> QuestionDraft.fromBank(100L + i, "질문 " + i, null, QuestionKind.TECH))
            .toList());
  }

  private static Map<String, Score> techScores(int s) {
    Map<String, Score> m = new LinkedHashMap<>();
    for (String c : Rubric.TECH.criteria()) {
      m.put(c, new Score(s, "근거 문장"));
    }
    return m;
  }

  private static Evaluation eval(Map<String, Score> scores) {
    return Evaluation.of(
        QuestionKind.TECH, scores, "잘한 점", "고칠 점", "방향", "answer-evaluation.v2", "m");
  }

  @Test
  void 녹음하면_올라감_상태이고_STT와_평가를_거친다() {
    Answer a = Answer.recorded(session(), 2, "answers/7/a.webm", "audio/webm");
    assertThat(a.getStatus()).isEqualTo(AnswerStatus.UPLOADED);
    assertThat(a.getSeq()).isEqualTo(2);

    a.transcribed("TCP는 연결 지향이고 UDP는 비연결입니다.", 4200);
    assertThat(a.getStatus()).isEqualTo(AnswerStatus.TRANSCRIBED);
    assertThat(a.getDurationMillis()).isEqualTo(4200);

    a.evaluated(eval(techScores(4)));
    assertThat(a.getStatus()).isEqualTo(AnswerStatus.EVALUATED);
    assertThat(a.getEvaluation().overall()).isEqualTo(4.0);
  }

  @Test
  void 없는_질문에는_답할_수_없다() {
    assertThatThrownBy(() -> Answer.recorded(session(), 4, "k", "audio/webm"))
        .isInstanceOf(DomainException.class)
        .extracting("code")
        .isEqualTo("QUESTION_NOT_FOUND");
  }

  @Test
  void 다시_녹음하면_글자와_평가를_지우고_버전이_오른다() {
    Answer a = Answer.recorded(session(), 1, "k1", "audio/webm");
    a.transcribed("첫 답변", 1000);
    a.evaluated(eval(techScores(3)));
    int before = a.getAudioVersion();

    a.reRecord("k2", "audio/ogg");

    assertThat(a.getStatus()).isEqualTo(AnswerStatus.UPLOADED);
    assertThat(a.getTranscript()).isNull();
    assertThat(a.getEvaluation()).isNull();
    assertThat(a.getAudioKey()).isEqualTo("k2");
    assertThat(a.getAudioVersion()).isEqualTo(before + 1);
  }

  @Test
  void 글자가_없으면_평가할_수_없고_실패하면_다시_시도할_수_있다() {
    Answer a = Answer.recorded(session(), 1, "k", "audio/webm");
    assertThatThrownBy(() -> a.evaluated(eval(techScores(3))))
        .isInstanceOf(DomainException.class)
        .extracting("code")
        .isEqualTo("NOT_TRANSCRIBED");

    a.failed("STT 실패");
    assertThat(a.getStatus()).isEqualTo(AnswerStatus.FAILED);
    a.retry();
    assertThat(a.getStatus()).isEqualTo(AnswerStatus.UPLOADED);
    assertThat(a.getFailureReason()).isNull();
  }

  @Test
  void 평가만_실패했으면_글자를_남기고_평가부터_다시() {
    Answer a = Answer.recorded(session(), 1, "k", "audio/webm");
    a.transcribed("답변 글자", 1000);
    a.failed("평가 실패");

    a.retry();

    assertThat(a.getStatus()).isEqualTo(AnswerStatus.TRANSCRIBED);
    assertThat(a.getTranscript()).isEqualTo("답변 글자");
  }

  @Test
  void 실패가_아니면_다시_시도할_수_없다() {
    Answer a = Answer.recorded(session(), 1, "k", "audio/webm");
    assertThatThrownBy(a::retry)
        .isInstanceOf(DomainException.class)
        .extracting("code")
        .isEqualTo("NOT_FAILED");
  }

  @Test
  void 평가는_루브릭_기준이_모두_있어야_하고_다른_기준은_버린다() {
    Map<String, Score> scores = techScores(5);
    scores.put("specificity", new Score(1, "인성 기준"));
    Evaluation e = eval(scores);
    assertThat(e.scores()).containsOnlyKeys(Rubric.TECH.criteria());

    Map<String, Score> missing = techScores(4);
    missing.remove("depth");
    assertThatThrownBy(() -> eval(missing))
        .isInstanceOf(DomainException.class)
        .extracting("code")
        .isEqualTo("INVALID_EVALUATION");
  }

  @Test
  void 점수는_1에서_5_근거가_없으면_3점까지() {
    Map<String, Score> out = techScores(4);
    out.put("accuracy", new Score(6, "근거"));
    assertThatThrownBy(() -> eval(out)).isInstanceOf(DomainException.class);

    Map<String, Score> noEvidence = techScores(4);
    noEvidence.put("depth", new Score(5, " "));
    Evaluation e = eval(noEvidence);
    assertThat(e.scores().get("depth").score()).isEqualTo(3);
    assertThat(e.overall()).isEqualTo(3.8); // (4+3+4+4)/4 = 3.75 → 3.8
  }

  @Test
  void 인성_질문은_인성_루브릭() {
    assertThat(Rubric.of(QuestionKind.BEHAVIORAL).criteria())
        .isEqualTo(List.of("specificity", "ownership", "collaboration", "reflection"));
  }
}
