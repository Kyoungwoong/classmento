package com.classmento.speech;

/** 테스트용 STT. 오디오 내용과 무관하게 고정 문장을 돌려준다. */
public class FakeSttClient implements SttClient {

  private final String text;

  public FakeSttClient(String text) {
    this.text = text;
  }

  @Override
  public Transcript transcribe(byte[] audio, String filename) {
    return new Transcript(text, 0);
  }

  @Override
  public boolean isAvailable() {
    return true;
  }
}
