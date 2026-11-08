# 백엔드 규칙

스택: Spring Boot 4.1 / Java 17 / Gradle(Kotlin DSL) / JPA / H2(local)·PostgreSQL(prod) / springdoc-openapi 3 / anthropic-java / Spotless(google-java-format).
Spring Boot 4는 3.x와 패키지가 다른 곳이 있다(예: 테스트 자동설정 `org.springframework.boot.webmvc.test.autoconfigure`). 확신이 없으면 context7 MCP로 문서를 확인한다.

## 구조
도메인별 패키지(`resume`, `interview`, `evaluation`) 안에 `Controller → Service → Repository`. 외부 연동은 `llm`, `speech`의 포트/어댑터. 상세: [architecture.md](../architecture.md)

## 규칙
- DTO·응답은 `record`. 엔티티를 API로 직접 내보내지 않는다.
- 요청 검증은 Bean Validation(`@Valid`, `@NotBlank` …). 에러는 `GlobalExceptionHandler`에서 `ApiError`로.
- 설정값은 `@ConfigurationProperties` record. `@Value` 남발 금지.
- 비밀값은 환경변수로만(`ANTHROPIC_API_KEY`, `CLOVA_SPEECH_*`). `application.yml`에 실제 값 금지.
- AI 호출은 `LlmClient`/`SttClient` 인터페이스로만. 테스트는 `FakeLlmClient`/`FakeSttClient`(src/test). **자동 테스트에서 실제 API 호출 금지.**
- 프롬프트는 `resources/prompts/` 파일로만 → [ai/interview-and-rubric.md](../ai/interview-and-rubric.md)
- 테스트: 서비스는 단위 테스트, 컨트롤러는 `@SpringBootTest + @AutoConfigureMockMvc`. 테스트 프로필 `test`(H2 메모리, AI 키 비움)가 자동 적용된다.
- 포맷: Spotless. 훅이 편집 시 자동 실행, verify에서 `spotlessCheck`.

## 명령 (backend/ 에서)
`./gradlew bootRun` · `./gradlew test` · `./gradlew spotlessApply` · `./gradlew spotlessCheck` · `./gradlew build`
