package com.classmento.interview.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** 세션 안의 질문(세션과 함께 저장·삭제). seq는 1부터. */
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

  protected InterviewQuestion() {}

  static InterviewQuestion of(int seq, QuestionDraft d) {
    InterviewQuestion q = new InterviewQuestion();
    q.seq = seq;
    q.text = d.text();
    q.intent = d.intent();
    q.kind = d.kind();
    q.bankQuestionId = d.bankQuestionId();
    q.generated = d.generated();
    return q;
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
}
