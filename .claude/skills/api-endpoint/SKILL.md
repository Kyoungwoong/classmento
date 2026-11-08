---
name: api-endpoint
description: classmento 백엔드(Spring Boot)에 REST 엔드포인트를 추가하거나 요청/응답 모양을 바꿀 때 사용. 컨트롤러·DTO·검증·테스트 작성 후 OpenAPI로 프론트 타입을 재생성해 계약을 맞춘다.
---

# API 엔드포인트 추가·변경

## 1. 설계
- `docs/api/README.md`의 경로·에러 규칙 확인. 경로는 `/api/<복수 리소스>`.
- 오래 걸리는 AI 작업은 `202 Accepted` + 상태 조회로.

## 2. 구현 — DDD 계층 (`backend/src/main/java/com/classmento/<context>/`, 규칙: `docs/conventions/ddd-tdd.md`)
**TDD 순서로, 각 단계를 red/green 커밋으로 나눈다.**
1. `domain/` — 규칙 테스트(Spring 없음) → 애그리거트·값 객체·리포지토리 인터페이스. 규칙 위반은 `DomainException`(→ 409).
2. `application/` — 유스케이스 테스트(가짜 포트) → `XxxService`. 입력은 커맨드 record. AI는 `LlmClient`/`EmbeddingClient` 포트.
3. `infrastructure/` — Spring Data 구현(`XxxJpaRepository extends JpaRepository, XxxRepository`), 벡터는 JdbcTemplate. 실제 DB 통합 테스트.
4. `presentation/` — `XxxController`(`@RestController`, `@Valid`)와 요청·응답 DTO record(엔티티 노출 금지, `@Schema(requiredProperties)`). MockMvc 테스트.
- 새 컨텍스트면 `ArchitectureTest.CONTEXTS`에 추가한다.
- Spring Boot 4 API가 불확실하면 context7 MCP로 확인한다.

## 3. 테스트
- domain: 순수 JUnit / application: `FakeLlmClient`·`FakeSttClient` 등 가짜 포트
- presentation: `IntegrationTest` 상속(`@SpringBootTest` + MockMvc). 성공 1개와 실패(검증·권한·404) 1개 이상.
- `cd backend && ./gradlew spotlessApply test` (ArchitectureTest 포함)

## 4. 계약 동기화 (생략 금지)
1. `cd backend && ./gradlew bootRun` (백그라운드)
2. `cd frontend && pnpm gen:api` → `src/lib/api/schema.d.ts` 갱신
3. `src/lib/api/client.ts`에 호출 함수 추가(타입은 `components["schemas"][...]`)
4. `pnpm typecheck` 통과
5. `docs/api/README.md`의 "현재 엔드포인트" 표 갱신
6. 계약이 의심되면 `contract-checker` 서브에이전트로 확인
