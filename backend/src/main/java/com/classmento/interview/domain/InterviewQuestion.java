package com.classmento.interview.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * 세션 안의 질문(세션과 함께 저장·삭제). seq는 1부터 붙는 고정 식별자다. 꼬리질문은 뒤 번호로 붙고, 화면 순서는 (rootSeq, depth)로 정한다(ADR
 * 0008).
 */
@Embeddable
public class InterviewQuestion {

  @Column(nullable = false)
  private int seq;

  @Column(nullable = false)
  private String text;

  private String intent;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private QuestionKind kind;

  private Long bankQuestionId;

  @Column(nullable = false)
  private boolean generated;

  /** 꼬리질문이면 바로 앞 질문 번호, 본 질문이면 null. */
  private Integer parentSeq;

  /** 사슬의 본 질문 번호(본 질문이면 자기 번호). */
  @Column(nullable = false)
  private int rootSeq;

  /** 0 = 본 질문, 1 = 첫 꼬리, 2 = 꼬리의 꼬리. */
  @Column(nullable = false)
  private int depth;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FollowUpState followUp = FollowUpState.NONE;

  protected InterviewQuestion() {}

  static InterviewQuestion of(int seq, QuestionDraft d, boolean followUpEligible) {
    InterviewQuestion q = new InterviewQuestion();
    q.seq = seq;
    q.text = d.text();
    q.intent = d.intent();
    q.kind = d.kind();
    q.bankQuestionId = d.bankQuestionId();
    q.generated = d.generated();
    q.rootSeq = seq;
    q.depth = 0;
    q.followUp = followUpEligible ? FollowUpState.AWAITING : FollowUpState.NONE;
    return q;
  }

  static InterviewQuestion followUpOf(
      int seq, InterviewQuestion parent, QuestionDraft d, boolean followUpEligible) {
    InterviewQuestion q = of(seq, d, followUpEligible);
    q.parentSeq = parent.seq;
    q.rootSeq = parent.rootSeq;
    q.depth = parent.depth + 1;
    return q;
  }

  void markAsked() {
    this.followUp = FollowUpState.ASKED;
  }

  void markClosed() {
    this.followUp = FollowUpState.CLOSED;
  }

  public int getSeq() {
    return seq;
  }

  public String getText() {
    return text;
  }

  public String getIntent() {
    return intent;
  }

  public QuestionKind getKind() {
    return kind;
  }

  public Long getBankQuestionId() {
    return bankQuestionId;
  }

  public boolean isGenerated() {
    return generated;
  }

  public Integer getParentSeq() {
    return parentSeq;
  }

  public int getRootSeq() {
    return rootSeq;
  }

  public int getDepth() {
    return depth;
  }

  public FollowUpState getFollowUp() {
    return followUp;
  }
}
