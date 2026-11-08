package com.classmento.speech;

/**
 * 음성→텍스트 포트. 구현체: {@link ClovaSttClient}, 키가 없을 때 {@link UnavailableSttClient}, 테스트는 FakeSttClient.
 */
public interface SttClient {

  Transcript transcribe(byte[] audio, String filename);

  boolean isAvailable();
}
