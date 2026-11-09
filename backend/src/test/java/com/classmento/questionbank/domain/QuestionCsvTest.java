package com.classmento.questionbank.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.classmento.questionbank.domain.QuestionCsv.LineError;
import com.classmento.questionbank.domain.QuestionCsv.Parsed;
import com.classmento.questionbank.domain.QuestionCsv.Row;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** CSV 대량 등록 형식: 헤더 고정, 직무·태그는 |, 줄별 오류(헤더가 1번 줄). */
class QuestionCsvTest {

  private static final String HEADER = "대분류,소분류,질문,난이도,직무,태그,의도\n";

  private static Parsed parse(String body) {
    return QuestionCsv.parse(HEADER + body);
  }

  private static List<Integer> errorLines(Parsed p) {
    return p.errors().stream().map(LineError::line).toList();
  }

  @Test
  void 정상_행을_읽는다_BOM과_CRLF도_괜찮다() {
    Parsed p =
        QuestionCsv.parse(
            "﻿대분류,소분류,질문,난이도,직무,태그,의도\r\n"
                + "CS,네트워크,TCP와 UDP의 차이를 설명해 주세요.,신입,백엔드|공통,TCP|UDP,기본기\r\n");

    assertThat(p.errors()).isEmpty();
    Row r = p.rows().get(0);
    assertThat(r.line()).isEqualTo(2);
    assertThat(r.top()).isEqualTo("CS");
    assertThat(r.sub()).isEqualTo("네트워크");
    assertThat(r.text()).isEqualTo("TCP와 UDP의 차이를 설명해 주세요.");
    assertThat(r.level()).isEqualTo(Level.NEW);
    assertThat(r.jobRoles()).containsExactlyInAnyOrder(JobRole.BACKEND, JobRole.COMMON);
    assertThat(r.tags()).containsExactly("TCP", "UDP");
    assertThat(r.intent()).isEqualTo("기본기");
  }

  @Test
  void 따옴표_안의_쉼표_줄바꿈_큰따옴표를_읽는다() {
    Parsed p =
        parse(
            "CS,네트워크,\"HTTP, HTTPS의 차이를\n\"\"보안\"\" 관점에서 말해 주세요.\",주니어,BACKEND,,\n"
                + "CS,보안,비밀번호 저장 방법을 말해 주세요.,JUNIOR,frontend,,\n");

    assertThat(p.errors()).isEmpty();
    assertThat(p.rows()).hasSize(2);
    assertThat(p.rows().get(0).text()).isEqualTo("HTTP, HTTPS의 차이를\n\"보안\" 관점에서 말해 주세요.");
    assertThat(p.rows().get(0).tags()).isEmpty();
    assertThat(p.rows().get(0).intent()).isNull();
    // 여러 줄 칸이 있어도 다음 행 번호는 실제 줄 번호
    assertThat(p.rows().get(1).line()).isEqualTo(4);
    assertThat(p.rows().get(1).jobRoles()).isEqualTo(Set.of(JobRole.FRONTEND));
  }

  @Test
  void 칸_중간의_따옴표는_글자_그대로다() {
    Parsed p = parse("CS,보안,5\" 화면에서 XSS를 막는 방법을 말해 주세요.,신입,공통,,\n");
    assertThat(p.errors()).isEmpty();
    assertThat(p.rows().get(0).text()).isEqualTo("5\" 화면에서 XSS를 막는 방법을 말해 주세요.");
  }

  @Test
  void 빈_줄은_건너뛴다() {
    Parsed p = parse("\nCS,보안,비밀번호 저장 방법을 말해 주세요.,신입,공통,,\n\n");
    assertThat(p.errors()).isEmpty();
    assertThat(p.rows()).hasSize(1);
  }

  @Test
  void 헤더가_다르면_1번_줄_오류() {
    Parsed p = QuestionCsv.parse("category,sub,text\nCS,보안,질문입니다아,신입,공통,,\n");
    assertThat(errorLines(p)).containsExactly(1);
    assertThat(p.errors().get(0).message()).contains("대분류,소분류,질문,난이도,직무,태그,의도");
  }

  @Test
  void 질문이_한_줄도_없으면_오류() {
    assertThat(QuestionCsv.parse(HEADER).errors())
        .extracting(LineError::message)
        .containsExactly("질문이 한 줄도 없어요.");
  }

  @Test
  void 행마다_틀린_곳을_모두_알려준다() {
    Parsed p =
        parse(
            "CS,보안,짧아,신입,공통,,\n" // 3: 질문 길이
                + "CS,보안,난이도가 이상한 질문,고수,공통,,\n" // 3: 난이도
                + "CS,보안,직무가 이상한 질문입니다,신입,요리사,,\n" // 4: 직무
                + ",보안,대분류가 빈 질문입니다,신입,공통,,\n" // 5: 대분류
                + "CS,보안,칸 수가 모자란 질문\n" // 6: 칸 수
                + "CS,보안,태그가 너무 많은 질문,신입,공통,a|b|c|d|e|f|g|h|i|j|k,\n" // 7: 태그
                + "CS,보안,직무가 빈 질문입니다,신입,,,\n" // 8: 직무 없음
                + "CS,보안,의도가 긴 질문입니다,신입,공통,,"
                + "가".repeat(201)
                + "\n"); // 9: 의도

    assertThat(p.rows()).isEmpty();
    assertThat(errorLines(p)).containsExactly(2, 3, 4, 5, 6, 7, 8, 9);
    assertThat(p.errors().get(0).message()).isEqualTo("질문은 5~300자로 입력해 주세요.");
    assertThat(p.errors().get(1).message()).contains("신입, 주니어, 미들, 시니어");
    assertThat(p.errors().get(2).message()).contains("요리사");
    assertThat(p.errors().get(4).message()).contains("7칸");
  }

  @Test
  void 닫히지_않은_따옴표는_오류() {
    Parsed p = parse("CS,보안,\"따옴표가 안 닫힌 질문,신입,공통,,\n");
    assertThat(errorLines(p)).containsExactly(2);
    assertThat(p.errors().get(0).message()).contains("따옴표");
  }

  @Test
  void 천_줄을_넘으면_오류() {
    String row = "CS,보안,비밀번호 저장 방법을 말해 주세요.,신입,공통,,\n";
    Parsed p = parse(row.repeat(QuestionCsv.MAX_ROWS + 1));
    assertThat(p.errors())
        .extracting(LineError::message)
        .containsExactly("한 번에 1000줄까지 올릴 수 있어요. 파일을 나눠 주세요.");
  }
}
