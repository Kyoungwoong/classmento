package com.classmento.resume;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

/** PDF(PDFBox), DOCX(Apache POI)에서 본문 텍스트를 뽑는다. 스캔 이미지 PDF(OCR 필요)는 빈 문자열이 나온다. */
@Component
public class TextExtractor {

  public String extract(ResumeFileType type, byte[] content) throws IOException {
    String raw =
        switch (type) {
          case PDF -> {
            try (PDDocument doc = Loader.loadPDF(content)) {
              yield new PDFTextStripper().getText(doc);
            }
          }
          case DOCX -> {
            try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(content));
                XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
              yield extractor.getText();
            }
          }
        };
    return normalize(raw);
  }

  /** 줄 끝 공백 제거, 3줄 이상 빈 줄은 2줄로. */
  static String normalize(String text) {
    return text.replace("\r\n", "\n")
        .replaceAll("[ \\t\\u00A0]+\\n", "\n")
        .replaceAll("\\n{3,}", "\n\n")
        .strip();
  }
}
