package com.classmento.resume.domain;

import java.util.Locale;
import java.util.Optional;

/** 지원 파일 형식. 확장자와 파일 앞부분(매직 바이트)을 함께 확인한다. */
public enum ResumeFileType {
  PDF("application/pdf"),
  DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

  private final String contentType;

  ResumeFileType(String contentType) {
    this.contentType = contentType;
  }

  public String contentType() {
    return contentType;
  }

  public static Optional<ResumeFileType> detect(String filename, byte[] head) {
    String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
    if (name.endsWith(".pdf") && startsWith(head, "%PDF")) {
      return Optional.of(PDF);
    }
    if (name.endsWith(".docx") && startsWith(head, "PK")) {
      return Optional.of(DOCX);
    }
    return Optional.empty();
  }

  public static ResumeFileType fromContentType(String contentType) {
    for (ResumeFileType t : values()) {
      if (t.contentType.equals(contentType)) {
        return t;
      }
    }
    throw new IllegalArgumentException("지원하지 않는 형식: " + contentType);
  }

  private static boolean startsWith(byte[] bytes, String prefix) {
    if (bytes.length < prefix.length()) {
      return false;
    }
    for (int i = 0; i < prefix.length(); i++) {
      if (bytes[i] != prefix.charAt(i)) {
        return false;
      }
    }
    return true;
  }
}
