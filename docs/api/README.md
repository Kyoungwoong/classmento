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
  | 400 | `INVALID_REQUEST` | 입력 형식·검증 실패 (message = 첫 번째 필드 에러 문장. 예: 이메일 형식 `a@b`) |
  | 401 | `LOGIN_FAILED` / (본문 없음) | 로그인 실패 / 로그인 필요 |
  | 403 | (본문 없음) | 권한 없음 또는 CSRF 토큰 누락 |
  | 404 | `NOT_FOUND` | 리소스 없음 (남의 리소스도 404) |
  | 409 | `EMAIL_TAKEN` / `RESUME_NOT_READY` / `RESUME_INVALID_STATE` / 도메인 규칙 코드 | 도메인 규칙 위반(`DomainException`): 이미 가입된 이메일, 파싱 전 이력서 수정 등 |
  | 413 | `FILE_TOO_LARGE` | 업로드 용량 초과 |
  | 503 | `AI_UNAVAILABLE` | Claude/OpenAI/CLOVA 미설정·실패 |
- 업로드: `multipart/form-data`, 필드명 `file`(이력서), `audio`(답변 녹음).
- 오래 걸리는 작업(이력서 처리, 질문 생성, 평가)은 `202 Accepted`로 시작하고 상태를 조회하는 방식이다. 스트리밍은 MVP 이후.
- 응답 record의 필수 필드는 `@Schema(requiredProperties = {...})`로 표시한다. 표시하지 않으면 생성 TS 타입이 전부 `?`(optional)가 된다.

## 인증 · CSRF
- 로그인 상태는 HttpOnly 세션 쿠키(`SESSION`)로 유지한다. 프론트는 같은 출처의 `/api`로만 호출한다(Next rewrites).
- 변경 요청(POST/PUT/PATCH/DELETE)에는 `X-XSRF-TOKEN` 헤더가 필요하다. 값은 `XSRF-TOKEN` 쿠키에 있고, 쿠키가 없으면 `GET /api/auth/csrf`를 먼저 호출한다. `client.ts`가 자동으로 처리한다.
- 공개 경로: `/api/health`, `/api/auth/**`, Swagger. `/api/admin/**`는 ADMIN만, 그 외 `/api/**`는 로그인이 필요하다.

## 현재 엔드포인트
| 메서드 | 경로 | 권한 | 설명 |
|---|---|---|---|
| GET | `/api/health` | 공개 | `{status, llmAvailable, sttAvailable, embeddingAvailable, fakeAi}` |
| GET | `/api/auth/csrf` | 공개 | CSRF 토큰 발급(쿠키 설정) |
| POST | `/api/auth/signup` | 공개 | `{email, password(8+), name}` → 201 `UserView` + 로그인 상태 |
| POST | `/api/auth/login` | 공개 | `{email, password}` → `UserView` |
| POST | `/api/auth/logout` | 공개 | 204 |
| GET | `/api/auth/me` | 공개 | `{authenticated, user?}` |
| GET | `/api/auth/providers` | 공개 | 설정된 소셜 로그인 `{providers:[{id,name}]}` → `/oauth2/authorization/{id}`로 이동 |
| POST | `/api/resumes` | 로그인 | multipart `file`(PDF/DOCX ≤20MB) → 202 `ResumeDetail` |
| GET | `/api/resumes` | 로그인 | 내 이력서 목록 `ResumeSummary[]` |
| GET | `/api/resumes/{id}` | 본인 | 상태 + `parsed`(PARSED일 때) |
| GET | `/api/resumes/{id}/experiences` | 본인 | 경험 목록 `ExperienceView[]`(seq 순: 경력 → 프로젝트) |
| PUT | `/api/resumes/{id}/parsed` | 본인 | 수정한 `ParsedResume` 저장 (PARSED 상태에서만, 아니면 409 `RESUME_NOT_READY`) |
| GET | `/api/admin/users` | ADMIN | 최근 가입 순 200명 |
