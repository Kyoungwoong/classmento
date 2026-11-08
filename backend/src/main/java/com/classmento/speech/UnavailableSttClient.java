package com.classmento.speech;

/** CLOVA_SPEECH_* 설정이 없을 때 등록된다. */
public class UnavailableSttClient implements SttClient {

  @Override
  public Transcript transcribe(byte[] audio, String filename) {
    throw new SttUnavailableException("CLOVA_SPEECH_INVOKE_URL / CLOVA_SPEECH_SECRET이 설정되지 않았습니다.");
  }

  @Override
  public boolean isAvailable() {
    return false;
  }
}
