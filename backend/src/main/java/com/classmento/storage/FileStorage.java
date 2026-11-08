package com.classmento.storage;

/** 업로드 파일 저장소 포트. 지금은 로컬 디스크, 운영에서는 S3 등으로 교체한다. */
public interface FileStorage {

  /**
   * @param directory 논리 폴더(예: "resumes/12")
   * @return 나중에 {@link #load}에 넘길 저장 키
   */
  String store(String directory, String filename, byte[] content);

  byte[] load(String key);
}
