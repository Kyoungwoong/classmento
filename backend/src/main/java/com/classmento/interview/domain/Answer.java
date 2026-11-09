package com.classmento.interview.domain;

/** 질문 하나에 대한 답변(녹음 → STT → 평가). */
public class Answer {

  public static Answer recorded(InterviewSession s, int seq, String audioKey, String contentType) {
    throw new UnsupportedOperationException("TODO");
  }

  public void transcribed(String text, long durationMillis) {}

  public void evaluated(Evaluation e) {}

  public void reRecord(String audioKey, String contentType) {}

  public void failed(String reason) {}

  public void retry() {}

  public AnswerStatus getStatus() {
    return null;
  }

  public int getSeq() {
    return 0;
  }

  public long getDurationMillis() {
    return 0;
  }

  public Evaluation getEvaluation() {
    return null;
  }

  public String getTranscript() {
    return null;
  }

  public String getAudioKey() {
    return null;
  }

  public int getAudioVersion() {
    return 0;
  }

  public String getFailureReason() {
    return null;
  }
}
