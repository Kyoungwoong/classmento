# API 계약

## 원칙
1. **원본은 백엔드 코드**. 컨트롤러 + DTO(`record`) + Bean Validation이 springdoc을 통해 `/v3/api-docs`로 나간다.
2. 프론트 타입은 **생성물만** 쓴다: `frontend/src/lib/api/schema.d.ts` (직접 수정 금지).
3. 백엔드 API를 바꾸면 같은 작업 안에서 `pnpm gen:api`로 타입을 재생성하고 프론트 컴파일을 통과시킨다.

## 재생성
```bash
cd backend && ./gradlew bootRun        # 8080
cd frontend && pnpm gen:api            # → src/lib/api/schema.d.ts
```
Swagger UI: http://localhost:8080/swagger-ui.html

## 규칙
- 경로: `/api/<복수형 리소스>` (예: `/api/resumes`, `/api/interviews/{id}/questions`).
- 에러: 항상 `ApiError { code, message }`. `message`는 사용자에게 그대로 보여줄 수 있는 한국어 문장.
  | HTTP | code | 상황 |
  |---|---|---|
  | 400 | `INVALID_REQUEST` | 검증 실패 |
  | 404 | `NOT_FOUND` | 리소스 없음 |
  | 413 | `FILE_TOO_LARGE` | 업로드 용량 초과 |
  | 503 | `AI_UNAVAILABLE` | Claude/CLOVA 미설정·실패 |
- 업로드: `multipart/form-data`, 필드명 `file`(이력서), `audio`(답변 녹음).
- 오래 걸리는 작업(질문 생성, 평가)은 `202 Accepted` + 상태 조회(`GET .../status`) 방식으로 시작한다. 스트리밍은 MVP 이후.

## 현재 엔드포인트
| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/health` | `{status, llmAvailable, sttAvailable}` |
