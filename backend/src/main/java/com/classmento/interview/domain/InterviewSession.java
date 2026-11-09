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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.BatchSize;

/**
 * 면접 세션 (애그리거트 루트). 질문은 만들 때 모두 정해 둔다(꼬리질문 없음). 규칙: 실행계획 2026-11-09-interview-session, ADR 0005.
 */
@Entity
@Table(name = "interview_session")
public class InterviewSession {

  public static final Set<Integer> COUNTS = Set.of(3, 5, 7);

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

  @ElementCollection(fetch = FetchType.EAGER)
  @BatchSize(size = 50) // 목록 조회 시 세션마다 질문을 따로 읽지 않게
  @CollectionTable(name = "interview_question", joinColumns = @JoinColumn(name = "session_id"))
  @OrderBy("seq")
  private List<InterviewQuestion> questions = new ArrayList<>();

  @Column(nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected InterviewSession() {}

  /** 질문을 고르기 전에 조건부터 확인한다(질문 은행·AI를 부르기 전에 거른다). */
  public static void requireValid(InterviewType type, int count, Conditions c) {
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
    requireValid(type, count, c);
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
    for (int i = 0; i < count; i++) {
      s.questions.add(InterviewQuestion.of(i + 1, drafts.get(i)));
    }
    return s;
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

  public List<InterviewQuestion> getQuestions() {
    return List.copyOf(questions);
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
