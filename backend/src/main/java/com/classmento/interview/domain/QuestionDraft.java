package com.classmento.interview.domain;

/** 세션을 만들 때 넣는 질문(질문 은행에서 고른 것 또는 AI가 만든 것). */
public record QuestionDraft(
    Long bankQuestionId, String text, String intent, QuestionKind kind, boolean generated) {

  public static QuestionDraft fromBank(Long id, String text, String intent, QuestionKind kind) {
    return new QuestionDraft(id, text, intent, kind, false);
  }

  public static QuestionDraft generated(String text, String intent, QuestionKind kind) {
    return new QuestionDraft(null, text, intent, kind, true);
  }
}
