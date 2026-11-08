package com.classmento.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

/** 테스트용 이력서 파일을 코드로 만든다(바이너리 fixture를 레포에 두지 않기 위해). */
public final class SampleFiles {

  /** PDF 기본 폰트(Helvetica)는 한글을 못 그리므로 PDF 샘플은 영문. */
  public static final String RESUME_TEXT =
      """
      Kim Example - Backend Developer
      Example Commerce, Backend Developer, 2022.03 - present
      Improved order API latency by 40 percent with Redis cache.
      Split payment module from the monolith to reduce blast radius.
      Skills: Java, Spring Boot, PostgreSQL, Redis, AWS
      """;

  public static final String RESUME_TEXT_KO =
      """
      김예시 - 백엔드 개발자

      예시커머스 백엔드 개발자 2022.03 - 현재
      Redis 캐시로 주문 API 응답 속도를 40% 개선했습니다.

      기술: Java, Spring Boot, PostgreSQL, Redis, AWS
      """;

  private SampleFiles() {}

  public static byte[] pdf(String text) {
    try (PDDocument doc = new PDDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      PDPage page = new PDPage();
      doc.addPage(page);
      try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
        content.beginText();
        content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
        content.setLeading(16);
        content.newLineAtOffset(50, 700);
        for (String line : text.strip().split("\n")) {
          content.showText(line);
          content.newLine();
        }
        content.endText();
      }
      doc.save(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static byte[] docx(String text) {
    try (XWPFDocument doc = new XWPFDocument();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      for (String line : text.strip().split("\n")) {
        doc.createParagraph().createRun().setText(line);
      }
      doc.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
