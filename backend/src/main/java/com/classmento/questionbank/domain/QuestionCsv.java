package com.classmento.questionbank.domain;

import java.util.List;
import java.util.Set;

/** 질문 CSV 형식(대량 등록). */
public final class QuestionCsv {

  public static final int MAX_ROWS = 1000;

  private QuestionCsv() {}

  public static Parsed parse(String content) {
    throw new UnsupportedOperationException("TODO");
  }

  public record Row(
      int line,
      String top,
      String sub,
      String text,
      Level level,
      Set<JobRole> jobRoles,
      List<String> tags,
      String intent) {}

  public record LineError(int line, String message) {}

  public record Parsed(List<Row> rows, List<LineError> errors) {}
}
