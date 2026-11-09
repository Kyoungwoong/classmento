package com.classmento.interview.domain;

import com.classmento.common.domain.DomainException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 질문 하나에 대한 답변(녹음 → STT → 평가). 세션과 따로 저장한다(STT·평가가 백그라운드에서 답변마다 따로 끝나기 때문).
 *
 * <p>다시 녹음하면 글자·평가를 지우고 {@code audioVersion}을 올린다. 백그라운드 처리는 시작할 때의 버전과 다르면 결과를 버린다(늦게 끝난 예전 녹음의
 * 결과가 새 녹음을 덮지 않게).
 */
@Entity
@Table(name = "interview_answer")
public class Answer {

  public static final int FAILURE_MAX = 300;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false)
  private Long sessionId;

  @Column(nullable = false, updatable = false)
  private Long userId;

  @Column(nullable = false, updatable = false)
  private int seq;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private QuestionKind kind;

  @Column(nullable = false)
  private String audioKey;

  @Column(nullable = false)
  private String contentType;

  @Column(nullable = false)
  private int audioVersion;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AnswerStatus status;

  private String transcript;

  private long durationMillis;

  private String failureReason;

  private Double overallScore;

  private String strength;

  private String improvement;

  private String suggestion;

  private String promptVersion;

  private String model;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "interview_answer_score", joinColumns = @JoinColumn(name = "answer_id"))
  @MapKeyColumn(name = "criterion")
  private Map<String, ScoreEntry> scores = new HashMap<>();

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(nullable = false)
  private Instant updatedAt = Instant.now();

  protected Answer() {}

  /** 진행 중인 면접의 있는 질문에만 답할 수 있다. */
  public static Answer recorded(
      InterviewSession session, int seq, String audioKey, String contentType) {
    InterviewQuestion q = session.requireAnswerable(seq);
    Answer a = new Answer();
    a.sessionId = session.getId();
    a.userId = session.getUserId();
    a.seq = seq;
    a.kind = q.getKind();
    a.audioKey = audioKey;
    a.contentType = contentType;
    a.audioVersion = 1;
    a.status = AnswerStatus.UPLOADED;
    return a;
  }

  /** 다시 녹음: 글자·평가·실패를 지우고 처음부터. */
  public void reRecord(String audioKey, String contentType) {
    this.audioKey = audioKey;
    this.contentType = contentType;
    this.audioVersion++;
    reset();
  }

  public void transcribed(String text, long durationMillis) {
    this.transcript = text == null ? "" : text.strip();
    this.durationMillis = Math.max(0, durationMillis);
    this.status = AnswerStatus.TRANSCRIBED;
  }

  public void evaluated(Evaluation e) {
    if (status != AnswerStatus.TRANSCRIBED) {
      throw new DomainException("NOT_TRANSCRIBED", "답변을 글자로 바꾼 뒤에 평가할 수 있어요.");
    }
    scores.clear();
    e.scores().forEach((k, v) -> scores.put(k, new ScoreEntry(v.score(), v.evidence())));
    this.overallScore = e.overall();
    this.strength = e.strength();
    this.improvement = e.improvement();
    this.suggestion = e.suggestion();
    this.promptVersion = e.promptVersion();
    this.model = e.model();
    this.status = AnswerStatus.EVALUATED;
  }

  public void failed(String reason) {
    String r = reason == null ? "알 수 없는 오류" : reason.strip();
    this.failureReason = r.length() > FAILURE_MAX ? r.substring(0, FAILURE_MAX) : r;
    this.status = AnswerStatus.FAILED;
  }

  /** 실패한 답변만 같은 녹음으로 다시 처리한다. */
  public void retry() {
    if (status != AnswerStatus.FAILED) {
      throw new DomainException("NOT_FAILED", "실패한 답변만 다시 시도할 수 있어요.");
    }
    this.audioVersion++;
    reset();
  }

  private void reset() {
    this.status = AnswerStatus.UPLOADED;
    this.transcript = null;
    this.durationMillis = 0;
    this.failureReason = null;
    this.overallScore = null;
    this.strength = null;
    this.improvement = null;
    this.suggestion = null;
    this.promptVersion = null;
    this.model = null;
    this.scores.clear();
  }

  @PreUpdate
  void touch() {
    updatedAt = Instant.now();
  }

  /** 평가가 끝났을 때만 값이 있다(루브릭 순서). */
  public Evaluation getEvaluation() {
    if (status != AnswerStatus.EVALUATED) {
      return null;
    }
    Map<String, Score> ordered = new LinkedHashMap<>();
    for (String c : Rubric.of(kind).criteria()) {
      ScoreEntry s = scores.get(c);
      if (s != null) {
        ordered.put(c, new Score(s.score, s.evidence));
      }
    }
    return new Evaluation(
        ordered, strength, improvement, suggestion, overallScore, promptVersion, model);
  }

  public Long getId() {
    return id;
  }

  public Long getSessionId() {
    return sessionId;
  }

  public Long getUserId() {
    return userId;
  }

  public int getSeq() {
    return seq;
  }

  public QuestionKind getKind() {
    return kind;
  }

  public String getAudioKey() {
    return audioKey;
  }

  public String getContentType() {
    return contentType;
  }

  public int getAudioVersion() {
    return audioVersion;
  }

  public AnswerStatus getStatus() {
    return status;
  }

  public String getTranscript() {
    return transcript;
  }

  public long getDurationMillis() {
    return durationMillis;
  }

  public String getFailureReason() {
    return failureReason;
  }

  /** 기준 하나의 점수 저장 형태(interview_answer_score). */
  @Embeddable
  public static class ScoreEntry {

    @Column(nullable = false)
    private int score;

    private String evidence;

    protected ScoreEntry() {}

    ScoreEntry(int score, String evidence) {
      this.score = score;
      this.evidence = evidence;
    }
  }
}
