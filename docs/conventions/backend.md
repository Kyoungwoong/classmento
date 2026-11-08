# 백엔드 규칙

스택:
- Spring Boot 4.1 / Spring Security 7 / Java 17 / Gradle(Kotlin DSL)
- JPA + JdbcTemplate(벡터) / PostgreSQL 17 + pgvector / Flyway / spring-session-jdbc
- springdoc-openapi 3 / anthropic-java / PDFBox 3 / Apache POI / Spotless(google-java-format)

Spring Boot 4에서는 3.x와 패키지가 다른 곳이 있다.
- 테스트 자동설정: `org.springframework.boot.webmvc.test.autoconfigure`
- Jackson 3: `tools.jackson.databind.ObjectMapper`

확신이 없으면 context7 MCP로 문서를 확인한다.

## 구조 — DDD
바운디드 컨텍스트(`identity`, `resume`, …) × 계층(`domain` / `application` / `infrastructure` / `presentation`).
외부 기술은 플랫폼 포트(`llm`·`embedding`·`speech`·`storage`)를 쓴다.
**규칙 전문과 TDD·커밋 방식: [ddd-tdd.md](ddd-tdd.md)**, 구조 그림: [architecture.md](../architecture.md)

## 규칙
- DTO·응답은 `record`로 만든다. 엔티티를 API로 직접 내보내지 않는다. 응답 record에는 `@Schema(requiredProperties = {...})`를 붙인다.
- 요청 검증은 Bean Validation(`@Valid`, `@NotBlank` …)으로 하고, `message`는 사용자에게 보여줄 한국어로 쓴다. 도메인 에러는 `ApiException`을 던진다 → `GlobalExceptionHandler`가 `ApiError`로 바꾼다.
- 설정값은 `@ConfigurationProperties` record로 받는다. `@Value` 남발 금지.
- 비밀값은 환경변수로만 받는다(`.env.example`). `application.yml`에 실제 값 금지. local 프로필의 기본 관리자 비밀번호는 로컬 전용이다.
- 스키마: Flyway `V{n}__설명.sql` 새 파일로만 바꾼다. 엔티티는 스키마를 따른다(`ddl-auto: validate`).
- 권한: 사용자 데이터 조회는 `findByIdAndUserId`로 하고, 없으면 `ApiException.notFound()`. 관리자 API는 `/api/admin/**` 아래에 둔다.
- 오래 걸리는 처리는 `TaskExecutor` 빈에 넘기고, 상태 컬럼을 단계별로 갱신한다(`ResumeProcessor` 참고). 테스트에서는 동기로 실행한다.
- AI 호출은 포트 인터페이스로만 한다.
  - 테스트는 test 프로필의 가짜 구현(`DevFakeLlmClient`, `HashEmbeddingClient`) 또는 단위 테스트용 `FakeLlmClient`/`FakeSttClient`를 쓴다.
  - **자동 테스트에서 실제 API 호출 금지.**
- 프롬프트는 `resources/prompts/` 파일로만 관리한다 → [ai/interview-and-rubric.md](../ai/interview-and-rubric.md)
- 테스트:
  - 단위 테스트는 순수 JUnit으로 쓴다.
  - 통합 테스트는 `support.IntegrationTest`를 상속한다(실제 Postgres `classmento_test`, 매번 TRUNCATE).
  - 테스트 파일(PDF/DOCX)은 `support.SampleFiles`로 코드에서 만든다. 바이너리 fixture는 커밋하지 않는다.
- 포맷: Spotless. 편집하면 훅이 자동으로 실행하고, verify에서 `spotlessCheck`로 확인한다.

## 명령 (backend/ 에서)
`bash ../scripts/db.sh up` · `./gradlew bootRun` · `./gradlew test` · `./gradlew spotlessApply` · `./gradlew build`
