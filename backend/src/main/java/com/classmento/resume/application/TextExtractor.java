package com.classmento.resume.application;

import com.classmento.resume.domain.ResumeFileType;
import java.io.IOException;

/** 파일 → 본문 텍스트 (애플리케이션 포트). 구현: infrastructure.PdfDocxTextExtractor (PDFBox, POI). */
public interface TextExtractor {

  /** 줄바꿈을 정리한 본문. 글자가 없는 파일(스캔 PDF 등)은 빈 문자열. */
  String extract(ResumeFileType type, byte[] content) throws IOException;
}
