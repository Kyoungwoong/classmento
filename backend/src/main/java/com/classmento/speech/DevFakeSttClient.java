package com.classmento.speech;

/**
 * 로컬 개발용 가짜 STT. 녹음 내용과 무관하게 고정 문장을 돌려준다. CLOVA 키 없이 녹음 → 평가 → 리포트 흐름을 확인하기 위한 것이며 prod에서는 등록되지
 * 않는다(classmento.ai.fake=false).
 */
public class DevFakeSttClient implements SttClient {

  static final String TEXT = "네, 그 부분은 먼저 문제 상황을 정리하고 원인을 찾은 다음 해결했습니다. (가짜 STT 답변입니다)";

  @Override
  public Transcript transcribe(byte[] audio, String filename) {
    return new Transcript(TEXT, 0);
  }

  @Override
  public boolean isAvailable() {
    return true;
  }
}
