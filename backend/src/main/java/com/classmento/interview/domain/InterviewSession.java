package com.classmento.interview.domain;

import com.classmento.common.domain.DomainException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.BatchSize;

/** 면접 세션 (애그리거트 루트). 본 질문은 만들 때 모두 정해 두고, 꼬리질문(옵션)은 답을 들은 뒤 뒤 번호로 붙인다. 규칙: ADR 0005, ADR 0008. */
@Entity
@Table(name = "interview_session")
public class InterviewSession {

  public static final Set<Integer> COUNTS = Set.of(3, 5, 7);

  /** 질문 하나에 붙일 수 있는 꼬리질문 최대 수(없음·한 번·두 번). */
  public static final int MAX_FOLLOW_UPS = 2;

  private static final Comparator<InterviewQuestion> DISPLAY_ORDER =
      Comparator.comparingInt(InterviewQuestion::getRootSeq)
          .thenComparingInt(InterviewQuestion::getDepth);

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, updatable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, updatable = false)
  private InterviewType type;

  @Enumerated(EnumType.STRING)
  private InterviewLevel level;

  @Enumerated(EnumType.STRING)
  private JobRole jobRole;

  private Long resumeId;

  private Long experienceId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SessionStatus status;

  /** 질문 하나에 붙일 꼬리질문 최대 수(0이면 꼬리 없음). 만든 뒤 바꾸지 않는다. */
  @Column(nullable = false, updatable = false)
  private int maxFollowUps;

  @ElementCollection(fetch = FetchType.EAGER)
  @BatchSize(size = 50) // 목록 조회 시 세션마다 질문을 따로 읽지 않게
  @CollectionTable(name = "interview_question", joinColumns = @JoinColumn(name = "session_id"))
  @OrderBy("seq")
  private List<InterviewQuestion> questions = new ArrayList<>();

  private Double reportOverall;

  private String reportStrength;

  private String reportImprovement;

  private String reportPromptVersion;

  private String reportModel;

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected InterviewSession() {}

  /** 질문을 고르기 전에 조건부터 확인한다(질문 은행·AI를 부르기 전에 거른다). */
  public static void requireValid(InterviewType type, int count, Conditions c) {
    requireValid(type, count, c, 0);
  }

  /** 꼬리질문 옵션까지 확인한다: 0~2, 인성 면접에는 켤 수 없다(ADR 0008). */
  public static void requireValid(InterviewType type, int count, Conditions c, int maxFollowUps) {
    if (maxFollowUps < 0 || maxFollowUps > MAX_FOLLOW_UPS) {
      throw new DomainException("INVALID_FOLLOW_UPS", "꼬리질문은 없음, 한 번, 두 번 중에서 골라 주세요.");
    }
    if (type == InterviewType.BEHAVIORAL && maxFollowUps > 0) {
      throw new DomainException("FOLLOW_UPS_NOT_ALLOWED", "인성 면접에는 꼬리질문을 붙이지 않아요.");
    }
    if (!COUNTS.contains(count)) {
      throw new DomainException("INVALID_QUESTION_COUNT", "질문 수는 3, 5, 7개 중에서 골라 주세요.");
    }
    switch (type) {
      case EXPERIENCE -> {
        if (c.resumeId() == null || c.experienceId() == null) {
          throw new DomainException("EXPERIENCE_REQUIRED", "경험 기반 면접은 이력서의 경험을 하나 골라 주세요.");
        }
      }
      case TECH, MIXED -> {
        if (c.level() == null || c.jobRole() == null) {
          throw new DomainException("CONDITIONS_REQUIRED", "직무와 연차를 골라 주세요.");
        }
        if (type == InterviewType.TECH && c.narrowByResume() && c.resumeId() == null) {
          throw new DomainException("RESUME_REQUIRED", "이력서 기술로 좁히려면 이력서를 골라 주세요.");
        }
      }
      case BEHAVIORAL -> {
        // 조건 없음
      }
    }
  }

  public static InterviewSession start(
      Long userId, InterviewType type, int count, Conditions c, List<QuestionDraft> drafts) {
    return start(userId, type, count, c, 0, drafts);
  }

  public static InterviewSession start(
      Long userId,
      InterviewType type,
      int count,
      Conditions c,
      int maxFollowUps,
      List<QuestionDraft> drafts) {
    requireValid(type, count, c, maxFollowUps);
    if (drafts.size() < count) {
      throw new DomainException("NOT_ENOUGH_QUESTIONS", "조건에 맞는 질문이 부족해요. 조건을 넓혀 주세요.");
    }
    Set<Long> seen = new HashSet<>();
    for (QuestionDraft d : drafts.subList(0, count)) {
      if (d.bankQuestionId() != null && !seen.add(d.bankQuestionId())) {
        throw new DomainException("DUPLICATE_QUESTION", "같은 질문이 두 번 들어갈 수 없어요.");
      }
    }
    InterviewSession s = new InterviewSession();
    s.userId = Objects.requireNonNull(userId);
    s.type = type;
    // 탭이 쓰는 조건만 저장한다. 쓰지 않는 이력서·경험 번호는 소유 확인을 거치지 않았으므로 남기지 않는다.
    boolean techLike = type == InterviewType.TECH || type == InterviewType.MIXED;
    s.level = techLike ? c.level() : null;
    s.jobRole = techLike ? c.jobRole() : null;
    s.resumeId =
        type == InterviewType.EXPERIENCE || (type == InterviewType.TECH && c.narrowByResume())
            ? c.resumeId()
            : null;
    s.experienceId = type == InterviewType.EXPERIENCE ? c.experienceId() : null;
    s.status = SessionStatus.IN_PROGRESS;
    s.maxFollowUps = maxFollowUps;
    for (int i = 0; i < count; i++) {
      QuestionDraft d = drafts.get(i);
      s.questions.add(InterviewQuestion.of(i + 1, d, s.followUpEligible(d.kind(), 0)));
    }
    return s;
  }

  /** 꼬리 대상: 옵션이 켜져 있고, 기술 질문이고, 아직 최대 깊이가 아니다. */
  private boolean followUpEligible(QuestionKind kind, int depth) {
    return maxFollowUps > 0 && kind == QuestionKind.TECH && depth < maxFollowUps;
  }

  /**
   * parentSeq 질문의 답을 듣고 꼬리질문을 붙인다. 번호는 지금 가장 큰 번호 + 1, 화면에서는 부모 바로 다음에 온다.
   *
   * @return 새 꼬리질문
   */
  public InterviewQuestion addFollowUp(int parentSeq, QuestionDraft draft) {
    if (status != SessionStatus.IN_PROGRESS) {
      throw new DomainException("INTERVIEW_CLOSED", "끝난 면접에는 꼬리질문을 붙일 수 없어요.");
    }
    InterviewQuestion parent = question(parentSeq);
    if (parent.getFollowUp() != FollowUpState.AWAITING) {
      throw new DomainException("FOLLOW_UP_DECIDED", "이 질문의 꼬리질문은 이미 정했어요.");
    }
    if (draft.kind() != QuestionKind.TECH || draft.text() == null || draft.text().isBlank()) {
      throw new DomainException("INVALID_FOLLOW_UP", "꼬리질문은 기술 질문 한 문장이어야 해요.");
    }
    String key = normalize(draft.text());
    if (questions.stream().anyMatch(q -> normalize(q.getText()).equals(key))) {
      throw new DomainException("DUPLICATE_QUESTION", "같은 질문이 두 번 들어갈 수 없어요.");
    }
    int seq = questions.stream().mapToInt(InterviewQuestion::getSeq).max().orElse(0) + 1;
    InterviewQuestion child =
        InterviewQuestion.followUpOf(
            seq, parent, draft, followUpEligible(draft.kind(), parent.getDepth() + 1));
    parent.markAsked();
    questions.add(child);
    return child;
  }

  /** 꼬리를 붙이지 않기로 한다. 이미 정한 질문이면 아무 일도 없다(같은 결과가 두 번 와도 안전). */
  public void closeFollowUp(int seq) {
    InterviewQuestion q = question(seq);
    if (q.getFollowUp() == FollowUpState.AWAITING) {
      q.markClosed();
    }
  }

  /**
   * 답을 받았는데(실패 제외) 아직 꼬리를 정하지 않은 질문 번호(화면 순서). 생성 중이거나, 중간에 끊겨 다시 판단해야 하는 질문이다.
   *
   * @param answers 질문 번호 → 답변 상태
   */
  public List<Integer> awaitingFollowUps(Map<Integer, AnswerStatus> answers) {
    return getQuestions().stream()
        .filter(q -> q.getFollowUp() == FollowUpState.AWAITING)
        .filter(
            q -> {
              AnswerStatus a = answers.get(q.getSeq());
              return a != null && a != AnswerStatus.FAILED;
            })
        .map(InterviewQuestion::getSeq)
        .toList();
  }

  /** 꼬리가 붙은 질문은 다시 녹음할 수 없다(꼬리가 이전 답을 근거로 만들어졌으므로). */
  public void requireReRecordable(int seq) {
    if (question(seq).getFollowUp() == FollowUpState.ASKED) {
      throw new DomainException("FOLLOW_UP_ASKED", "이 답변을 바탕으로 꼬리질문을 이미 했어요. 다시 녹음할 수 없어요.");
    }
  }

  /** 사용자가 고른 본 질문 수(꼬리 제외). */
  public int rootCount() {
    return (int) questions.stream().filter(q -> q.getDepth() == 0).count();
  }

  private static String normalize(String text) {
    return text == null ? "" : text.replaceAll("\\s+", " ").trim();
  }

  /**
   * 모든 질문에 답한 뒤에만, 한 번만 끝낼 수 있다(건너뛰기 없음). 끝나면 다시 녹음할 수 없으므로, 아직 글자로 바꾸는 중이거나 실패한 답변이 있으면 끝내지 않는다(끝난
   * 뒤 실패하면 빠져나올 길이 없다).
   *
   * @param answers 질문 번호 → 답변 상태
   */
  public void finish(Map<Integer, AnswerStatus> answers) {
    if (status != SessionStatus.IN_PROGRESS) {
      throw new DomainException("ALREADY_FINISHED", "이미 끝난 면접이에요.");
    }
    for (InterviewQuestion q : questions) {
      AnswerStatus a = answers.get(q.getSeq());
      if (a == null) {
        throw new DomainException("NOT_ALL_ANSWERED", "모든 질문에 답한 뒤에 끝낼 수 있어요.");
      }
      if (a == AnswerStatus.FAILED) {
        throw new DomainException("ANSWERS_FAILED", "다시 녹음해야 하는 답변이 있어요. 해당 질문에 다시 답해 주세요.");
      }
      if (a == AnswerStatus.UPLOADED) {
        throw new DomainException("ANSWERS_PROCESSING", "답변을 아직 정리하고 있어요. 잠시 후 다시 끝내 주세요.");
      }
    }
    if (!awaitingFollowUps(answers).isEmpty()) {
      throw new DomainException("FOLLOW_UP_PENDING", "다음 질문을 고르고 있어요. 잠시 후 다시 끝내 주세요.");
    }
    status = SessionStatus.FINISHED;
  }

  /** 모든 답변 평가가 끝난 뒤 종합을 붙인다(끝난 면접에 한 번). */
  public void report(SessionReport report) {
    if (status != SessionStatus.FINISHED) {
      throw new DomainException("NOT_FINISHED", "끝난 면접만 종합할 수 있어요.");
    }
    this.reportOverall = report.overall();
    this.reportStrength = report.strength();
    this.reportImprovement = report.improvement();
    this.reportPromptVersion = report.promptVersion();
    this.reportModel = report.model();
    this.status = SessionStatus.REPORTED;
  }

  public SessionReport getReport() {
    if (status != SessionStatus.REPORTED) {
      return null;
    }
    return new SessionReport(
        reportOverall, reportStrength, reportImprovement, reportPromptVersion, reportModel);
  }

  /** 진행 중인 면접의 있는 질문인지 확인하고 그 질문을 돌려준다. */
  public InterviewQuestion requireAnswerable(int seq) {
    if (status != SessionStatus.IN_PROGRESS) {
      throw new DomainException("INTERVIEW_CLOSED", "끝난 면접에는 답할 수 없어요.");
    }
    return question(seq);
  }

  /** seq번 질문(없으면 QUESTION_NOT_FOUND). */
  public InterviewQuestion question(int seq) {
    return questions.stream()
        .filter(q -> q.getSeq() == seq)
        .findFirst()
        .orElseThrow(() -> new DomainException("QUESTION_NOT_FOUND", "없는 질문이에요."));
  }

  public Long getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public InterviewType getType() {
    return type;
  }

  public InterviewLevel getLevel() {
    return level;
  }

  public JobRole getJobRole() {
    return jobRole;
  }

  public Long getResumeId() {
    return resumeId;
  }

  public Long getExperienceId() {
    return experienceId;
  }

  public SessionStatus getStatus() {
    return status;
  }

  /** 화면 순서(본 질문 → 그 꼬리들 → 다음 본 질문). */
  public List<InterviewQuestion> getQuestions() {
    return questions.stream().sorted(DISPLAY_ORDER).toList();
  }

  public int getMaxFollowUps() {
    return maxFollowUps;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
