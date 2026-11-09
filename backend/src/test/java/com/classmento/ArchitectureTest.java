package com.classmento;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * DDD 구조 규칙 (docs/conventions/ddd-tdd.md). 바운디드 컨텍스트 × 계층 의존 방향을 강제한다.
 *
 * <pre>presentation → application → domain ← infrastructure, 컨텍스트끼리 직접 의존 금지</pre>
 *
 * 새 컨텍스트(questionbank, interview …)를 만들면 CONTEXTS에 추가한다.
 */
@AnalyzeClasses(packages = "com.classmento", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  private static final String[] CONTEXTS = {"identity", "resume", "questionbank"};

  private static String[] contextPackages() {
    String[] packages = new String[CONTEXTS.length];
    for (int i = 0; i < CONTEXTS.length; i++) {
      packages[i] = "com.classmento." + CONTEXTS[i] + "..";
    }
    return packages;
  }

  @ArchTest
  static final ArchRule 컨텍스트_클래스는_네_계층_중_하나에_있다 =
      classes()
          .that()
          .resideInAnyPackage(contextPackages())
          .and()
          .doNotHaveSimpleName("package-info")
          .should()
          .resideInAnyPackage(
              "..domain..", "..application..", "..infrastructure..", "..presentation..")
          .because("컨텍스트 코드는 domain/application/infrastructure/presentation 중 한 곳에 둔다");

  @ArchTest
  static final ArchRule 도메인은_다른_계층과_Spring에_의존하지_않는다 =
      noClasses()
          .that()
          .resideInAPackage("com.classmento.*.domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..application..", "..infrastructure..", "..presentation..", "org.springframework..")
          .because("도메인 규칙은 프레임워크·바깥 계층과 무관해야 단위 테스트로 검증할 수 있다");

  @ArchTest
  static final ArchRule 애플리케이션은_인프라와_프레젠테이션에_의존하지_않는다 =
      noClasses()
          .that()
          .resideInAPackage("com.classmento.*.application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "com.classmento.*.infrastructure..", "com.classmento.*.presentation..")
          .because("유스케이스는 포트(인터페이스)에만 의존하고 구현은 infrastructure가 맡는다");

  /**
   * 프레젠테이션은 저장소 구현(persistence·JPA·JDBC)을 직접 쓰지 않는다. 예외: 인증 배관(infrastructure.security의 설정 record
   * 등)은 컨트롤러가 참조할 수 있다 — docs/conventions/ddd-tdd.md §2.
   */
  @ArchTest
  static final ArchRule 프레젠테이션은_저장소_구현에_의존하지_않는다 =
      noClasses()
          .that()
          .resideInAPackage("com.classmento.*.presentation..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "com.classmento.*.infrastructure.persistence..",
              "org.springframework.data..",
              "org.springframework.jdbc..")
          .orShould()
          .dependOnClassesThat()
          .haveSimpleNameEndingWith("JpaRepository")
          .because("데이터 접근은 application 유스케이스를 거친다");

  @ArchTest
  static final ArchRule identity는_resume에_의존하지_않는다 =
      noClasses()
          .that()
          .resideInAPackage("com.classmento.identity..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.classmento.resume..");

  @ArchTest
  static final ArchRule resume은_identity에_의존하지_않는다 =
      noClasses()
          .that()
          .resideInAPackage("com.classmento.resume..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.classmento.identity..")
          .because("다른 컨텍스트의 사용자 정보는 common.security.AuthUser와 userId로만 받는다");

  @ArchTest
  static final ArchRule questionbank는_다른_컨텍스트에_의존하지_않는다 =
      noClasses()
          .that()
          .resideInAPackage("com.classmento.questionbank..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("com.classmento.identity..", "com.classmento.resume..")
          .because("질문 은행은 독립 컨텍스트다(작성자는 userId로만 기록)");
}
