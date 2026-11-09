package com.classmento.questionbank.domain;

import com.classmento.common.domain.DomainException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 질문 CSV 형식(대량 등록). 헤더 고정, UTF-8(BOM 허용), RFC 4180식 따옴표(칸 안의 쉼표·줄바꿈·"" 허용). 직무·태그는 | 로 구분한다.
 * 난이도·직무는 한글 이름이나 영문 코드(JUNIOR, BACKEND) 모두 받는다.
 *
 * <p>여기서는 형식만 본다. 카테고리가 실제로 있는지는 가져오기 유스케이스가 확인한다. 오류는 행마다 첫 번째 하나만 알려준다(헤더가 1번 줄).
 */
public final class QuestionCsv {

  public static final int MAX_ROWS = 1000;
  public static final String HEADER = "대분류,소분류,질문,난이도,직무,태그,의도";
  private static final int COLUMNS = 7;

  private static final Map<String, Level> LEVELS = new LinkedHashMap<>();
  private static final Map<String, JobRole> JOB_ROLES = new LinkedHashMap<>();

  static {
    LEVELS.put("신입", Level.NEW);
    LEVELS.put("주니어", Level.JUNIOR);
    LEVELS.put("미들", Level.MIDDLE);
    LEVELS.put("시니어", Level.SENIOR);
    JOB_ROLES.put("백엔드", JobRole.BACKEND);
    JOB_ROLES.put("프론트엔드", JobRole.FRONTEND);
    JOB_ROLES.put("풀스택", JobRole.FULLSTACK);
    JOB_ROLES.put("모바일", JobRole.MOBILE);
    JOB_ROLES.put("데이터", JobRole.DATA);
    JOB_ROLES.put("devops", JobRole.DEVOPS);
    JOB_ROLES.put("공통", JobRole.COMMON);
  }

  private QuestionCsv() {}

  public static Parsed parse(String content) {
    String text = content.startsWith("﻿") ? content.substring(1) : content;
    Records records = split(text.replace("\r\n", "\n").replace('\r', '\n'));

    List<LineError> errors = new ArrayList<>();
    List<Row> rows = new ArrayList<>();
    List<Record> data = records.list();
    if (data.isEmpty() || !HEADER.equals(String.join(",", data.get(0).trimmedFields()))) {
      errors.add(new LineError(1, "첫 줄은 " + HEADER + " 이어야 해요. 예시 파일의 첫 줄을 그대로 써 주세요."));
      return new Parsed(rows, errors);
    }
    List<Record> body = data.subList(1, data.size());
    if (body.size() > MAX_ROWS) {
      errors.add(new LineError(body.get(MAX_ROWS).line(), "한 번에 1000줄까지 올릴 수 있어요. 파일을 나눠 주세요."));
      return new Parsed(rows, errors);
    }
    for (Record r : body) {
      try {
        rows.add(toRow(r));
      } catch (RowError e) {
        errors.add(new LineError(r.line(), e.getMessage()));
      }
    }
    if (records.unclosedQuoteLine() > 0) {
      errors.add(new LineError(records.unclosedQuoteLine(), "큰따옴표가 닫히지 않았어요. 칸을 감싼 따옴표를 확인해 주세요."));
    }
    if (rows.isEmpty() && errors.isEmpty()) {
      errors.add(new LineError(2, "질문이 한 줄도 없어요."));
    }
    return new Parsed(rows, errors);
  }

  private static Row toRow(Record r) {
    List<String> f = r.trimmedFields();
    if (f.size() != COLUMNS) {
      throw new RowError("칸이 7칸이어야 해요(지금 " + f.size() + "칸). 쉼표가 들어간 칸은 큰따옴표로 감싸 주세요.");
    }
    String top = required(f.get(0), "대분류를 입력해 주세요.");
    String sub = required(f.get(1), "소분류를 입력해 주세요.");
    String text = f.get(2);
    if (text.length() < Question.TEXT_MIN || text.length() > Question.TEXT_MAX) {
      throw new RowError("질문은 5~300자로 입력해 주세요.");
    }
    Level level = level(f.get(3));
    Set<JobRole> jobRoles = jobRoles(f.get(4));
    List<String> tags = tags(f.get(5));
    String intent = f.get(6).isEmpty() ? null : f.get(6);
    if (intent != null && intent.length() > Question.INTENT_MAX) {
      throw new RowError("질문 의도는 200자까지예요.");
    }
    return new Row(r.line(), top, sub, text, level, jobRoles, tags, intent);
  }

  private static String required(String value, String message) {
    if (value.isEmpty()) {
      throw new RowError(message);
    }
    return value;
  }

  private static Level level(String raw) {
    Level byLabel = LEVELS.get(raw);
    if (byLabel != null) {
      return byLabel;
    }
    try {
      return Level.valueOf(raw.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new RowError("난이도는 신입, 주니어, 미들, 시니어 중 하나로 써 주세요.");
    }
  }

  private static Set<JobRole> jobRoles(String raw) {
    Set<JobRole> roles = new LinkedHashSet<>();
    for (String name : pipe(raw)) {
      JobRole role = JOB_ROLES.get(name.toLowerCase(Locale.ROOT));
      if (role == null) {
        try {
          role = JobRole.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
          throw new RowError(
              "알 수 없는 직무예요: " + name + " (백엔드, 프론트엔드, 풀스택, 모바일, 데이터, DevOps, 공통 중에서 | 로 나눠 써 주세요)");
        }
      }
      roles.add(role);
    }
    if (roles.isEmpty()) {
      throw new RowError("직무를 하나 이상 써 주세요.");
    }
    return roles;
  }

  /** 같은 태그(대소문자·공백 무시)는 한 번만 */
  private static List<String> tags(String raw) {
    Map<String, String> byKey = new LinkedHashMap<>();
    for (String name : pipe(raw)) {
      try {
        Tag tag = Tag.of(name);
        byKey.putIfAbsent(tag.getKey(), tag.getName());
      } catch (DomainException e) {
        throw new RowError("태그 '" + name + "': " + e.getMessage());
      }
    }
    if (byKey.size() > Question.TAGS_MAX) {
      throw new RowError("태그는 10개까지 붙일 수 있어요.");
    }
    return List.copyOf(byKey.values());
  }

  private static List<String> pipe(String raw) {
    List<String> out = new ArrayList<>();
    for (String part : raw.split("\\|")) {
      String p = part.strip();
      if (!p.isEmpty()) {
        out.add(p);
      }
    }
    return out;
  }

  /** 줄바꿈이 정리된 본문을 레코드로 나눈다. 빈 줄은 버린다. */
  private static Records split(String text) {
    List<Record> records = new ArrayList<>();
    List<String> fields = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    boolean quoted = false;
    int line = 1;
    int recordLine = 1;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (quoted) {
        if (c == '"') {
          if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
            field.append('"');
            i++;
          } else {
            quoted = false;
          }
        } else {
          if (c == '\n') {
            line++;
          }
          field.append(c);
        }
      } else if (c == '"' && field.isEmpty()) {
        // 따옴표는 칸 시작에서만 감싸기로 본다. 칸 중간(예: 5" 화면)은 글자 그대로
        quoted = true;
      } else if (c == ',') {
        fields.add(field.toString());
        field.setLength(0);
      } else if (c == '\n') {
        fields.add(field.toString());
        field.setLength(0);
        addRecord(records, recordLine, fields);
        fields = new ArrayList<>();
        line++;
        recordLine = line;
      } else {
        field.append(c);
      }
    }
    if (quoted) {
      return new Records(records, recordLine);
    }
    fields.add(field.toString());
    addRecord(records, recordLine, fields);
    return new Records(records, 0);
  }

  private static void addRecord(List<Record> records, int line, List<String> fields) {
    boolean blank = fields.size() == 1 && fields.get(0).isBlank();
    if (!blank) {
      records.add(new Record(line, fields));
    }
  }

  private record Record(int line, List<String> fields) {
    List<String> trimmedFields() {
      return fields.stream().map(String::strip).toList();
    }
  }

  private record Records(List<Record> list, int unclosedQuoteLine) {}

  private static final class RowError extends RuntimeException {
    RowError(String message) {
      super(message, null, false, false);
    }
  }

  /** line: 파일의 실제 줄 번호(헤더 = 1). 여러 줄 칸이 있으면 그 행이 시작한 줄. */
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

  /** rows: 형식이 맞는 행(오류가 있어도 남겨 둔다 → 카테고리 확인까지 한 번에). ok()가 아니면 저장하지 않는다. */
  public record Parsed(List<Row> rows, List<LineError> errors) {

    public boolean ok() {
      return errors.isEmpty();
    }
  }
}
