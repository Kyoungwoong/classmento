# backend

규칙 전문: [../docs/conventions/backend.md](../docs/conventions/backend.md) · 구조: [../docs/architecture.md](../docs/architecture.md)

## 패키지 (`com.classmento`)
- `common` — CORS, `ApiError`/`ApiException`/`GlobalExceptionHandler`, `AiProperties`, `HealthController`
- `auth` — `AppUser`, `AuthUser`(세션 principal), `SecurityConfig`, `AuthController`, `SocialUserService`, `AdminSeeder`
- `admin` — `/api/admin/**` (ROLE_ADMIN)
- `llm` — `LlmClient` / `ClaudeLlmClient` / `DevFakeLlmClient` / `UnavailableLlmClient` / `PromptLoader`
- `embedding` — `EmbeddingClient` / `OpenAiEmbeddingClient` / `HashEmbeddingClient` / `PgVectors`
- `speech` — `SttClient` / `ClovaSttClient` / `UnavailableSttClient`
- `storage` — `FileStorage` / `LocalFileStorage`
- `resume` — 업로드·추출·청크·임베딩·구조화, `ResumeChunkRepository.findNearest`(kNN)
- `interview`, `evaluation` — 예정

## 꼭 지킬 것
- Spring Boot **4.1** / Spring Security 7 / Jackson **3**(`tools.jackson.*`). 패키지가 3.x와 다르니 모르면 context7로 확인한다.
- DB는 **PostgreSQL 17 + pgvector**(`localhost:5433`). 실행·테스트 전에 `bash ../scripts/db.sh up`.
- 스키마는 `src/main/resources/db/migration/V{n}__*.sql` **새 파일**로만 바꾼다. `ddl-auto: validate`라 엔티티와 어긋나면 기동이 실패한다.
- 벡터 컬럼은 JPA로 매핑하지 않는다. JdbcTemplate + `?::vector` + `PgVectors.toLiteral`.
- 도메인 코드는 외부 SDK를 직접 import하지 않는다 → 포트 인터페이스만 쓴다.
- 현재 사용자는 `@AuthenticationPrincipal AuthUser user`로 받는다. 조회는 `findByIdAndUserId`(남의 것은 404).
- 응답 record는 `@Schema(requiredProperties = {...})`로 필수 필드를 표시한다(FE 타입 정확도).
- 프롬프트: `prompts/<name>.vN.md` + `PromptLoader.load(...)`. 가짜 응답은 `fake/<name>.vN.json`이다(새 프롬프트를 만들면 함께 추가).
- 테스트:
  - `test` 프로필이 자동 적용된다: `classmento_test` DB, 가짜 AI, 동기 처리.
  - 통합 테스트는 `support/IntegrationTest`를 상속한다(매 테스트 TRUNCATE).
  - 로그인 사용자는 `.with(user(authUser))`, 변경 요청은 `.with(csrf())`를 붙인다.
- 컨트롤러를 추가·변경하면 `api-endpoint` 스킬 절차(OpenAPI → `pnpm gen:api`)를 따른다.
- 백그라운드 `bootRun`을 멈춘 뒤에도 java 프로세스가 8080을 점유할 수 있다 → 포트를 쓰는 프로세스를 종료한다.

## 명령
`./gradlew bootRun` · `./gradlew test` · `./gradlew spotlessApply` · `./gradlew build`
