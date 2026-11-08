# backend

규칙 전문: [../docs/conventions/backend.md](../docs/conventions/backend.md) · 구조: [../docs/architecture.md](../docs/architecture.md)

## 패키지 (`com.classmento`)
- `common` — CORS, `ApiError`, `GlobalExceptionHandler`, `HealthController`
- `llm` — `LlmClient`(포트) / `ClaudeLlmClient` / `UnavailableLlmClient` / `PromptLoader`
- `speech` — `SttClient`(포트) / `ClovaSttClient` / `UnavailableSttClient`
- `resume`, `interview`, `evaluation` — 도메인 (Controller → Service → Repository, DTO는 record)

## 꼭 지킬 것
- Spring Boot **4.1** (3.x 아님). 패키지 이동이 있으니 모르면 context7로 확인.
- 도메인 코드는 Anthropic SDK·CLOVA를 직접 import하지 않는다 → `LlmClient`/`SttClient`만.
- 프롬프트는 `src/main/resources/prompts/<name>.vN.md` + `PromptLoader.load("<name>.vN")`.
- 테스트는 `test` 프로필(H2 메모리, AI 키 비움)이 자동 적용. `FakeLlmClient`/`FakeSttClient` 사용.
- 컨트롤러를 추가·변경하면 `api-endpoint` 스킬 절차(OpenAPI → `pnpm gen:api`)를 따른다.

## 명령
`./gradlew bootRun` · `./gradlew test` · `./gradlew spotlessApply` · `./gradlew build`
