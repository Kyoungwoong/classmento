---
name: api-endpoint
description: classmento 백엔드(Spring Boot)에 REST 엔드포인트를 추가하거나 요청/응답 모양을 바꿀 때 사용. 컨트롤러·DTO·검증·테스트 작성 후 OpenAPI로 프론트 타입을 재생성해 계약을 맞춘다.
---

# API 엔드포인트 추가·변경

## 1. 설계
- `docs/api/README.md`의 경로·에러 규칙 확인. 경로는 `/api/<복수 리소스>`.
- 오래 걸리는 AI 작업은 `202 Accepted` + 상태 조회로.

## 2. 구현 (`backend/src/main/java/com/classmento/<domain>/`)
- `XxxController` — `@RestController`, `@RequestMapping("/api/...")`, `@Valid`.
- 요청/응답 DTO는 `record`. 엔티티 노출 금지. 필수 필드는 Bean Validation.
- `XxxService` — 유스케이스. AI는 `LlmClient`/`SttClient` 주입.
- 에러는 도메인 예외 → `GlobalExceptionHandler`에서 `ApiError(code, 한국어 message)`.
- Spring Boot 4 API가 불확실하면 context7 MCP로 확인.

## 3. 테스트
- 서비스: 순수 단위 테스트 + `FakeLlmClient`/`FakeSttClient`.
- 컨트롤러: `@SpringBootTest` + `@AutoConfigureMockMvc`(`org.springframework.boot.webmvc.test.autoconfigure`). 성공 1 + 검증 실패 1 이상.
- `cd backend && ./gradlew spotlessApply test`

## 4. 계약 동기화 (생략 금지)
1. `cd backend && ./gradlew bootRun` (백그라운드)
2. `cd frontend && pnpm gen:api` → `src/lib/api/schema.d.ts` 갱신
3. `src/lib/api/client.ts`에 호출 함수 추가(타입은 `components["schemas"][...]`)
4. `pnpm typecheck` 통과
5. `docs/api/README.md`의 "현재 엔드포인트" 표 갱신
6. 계약이 의심되면 `contract-checker` 서브에이전트로 확인
