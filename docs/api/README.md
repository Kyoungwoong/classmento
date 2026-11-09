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
| POST | `/api/auth/signup` | 공개 | `{email(≤254자), password(8+), name}` → 201 `UserView` + 로그인 상태. 형식·길이 오류는 400 |
| POST | `/api/auth/login` | 공개 | `{email, password}` → `UserView` |
| POST | `/api/auth/logout` | 공개 | 204 |
| GET | `/api/auth/me` | 공개 | `{authenticated, user?}` |
| GET | `/api/auth/providers` | 공개 | 설정된 소셜 로그인 `{providers:[{id,name}]}` → `/oauth2/authorization/{id}`로 이동 |
| POST | `/api/resumes` | 로그인 | multipart `file`(PDF/DOCX ≤20MB) → 202 `ResumeDetail` |
| GET | `/api/resumes` | 로그인 | 내 이력서 목록 `ResumeSummary[]` |
| GET | `/api/resumes/{id}` | 본인 | 상태 + `parsed`(PARSED일 때) |
| GET | `/api/resumes/{id}/experiences` | 본인 | 경험 목록 `ExperienceView[]`(seq 순: 경력 → 프로젝트) |
| PUT | `/api/resumes/{id}/parsed` | 본인 | 수정한 `ParsedResume` 저장 + 경험 재생성 (PARSED 상태에서만, 아니면 409 `RESUME_NOT_READY`). 임베딩 장애 시 503 `AI_UNAVAILABLE`이고 아무것도 바뀌지 않음. 같은 이력서 동시 저장은 순서대로 처리 |
| GET | `/api/admin/users` | ADMIN | 최근 가입 순 200명 |
| GET | `/api/admin/categories` | ADMIN | 질문 은행 카테고리 트리(대분류 → 소분류, 정렬순, 숨김 포함). 소분류마다 `questionCount`(숨긴 질문 포함) |
| POST | `/api/admin/categories` | ADMIN | `{parentId?, name(1~30)}` → 201. 형제 이름 중복 409 `CATEGORY_DUPLICATE`, 소분류 아래 409 `CATEGORY_TOO_DEEP`, 숨긴 대분류 아래 409 `CATEGORY_HIDDEN`, 없는 부모 404 |
| PATCH | `/api/admin/categories/{id}` | ADMIN | `{name?, sortOrder?, active?}` 바꿀 것만. 삭제는 없고 숨김(`active:false`) |
| GET | `/api/admin/tags?query=` | ADMIN | 태그 자동완성(대소문자 무시, 앞부분 일치 먼저, 최대 10개) |
| POST | `/api/admin/tags` | ADMIN | `{name(1~20, 쉼표 불가)}` → 새로 만들면 201, 같은 태그(대소문자·공백 무시)가 있으면 200 + 기존 것 `{tag, created}` |
| GET | `/api/admin/questions?topCategoryId=&categoryId=&level=&jobRole=&tagId=&q=&includeHidden=&page=&size=` | ADMIN | 질문 검색(최신순, 조건 AND, `q`는 본문 부분 일치). 숨긴 질문은 `includeHidden=true`일 때만. `size` 1~100(기본 20) → `{items, total, page, size}` |
| POST | `/api/admin/questions` | ADMIN | `{categoryId, text(5~300), level, jobRoles[1+], tagIds[≤10], intent?(≤200)}` → 201 `QuestionView`(대분류·소분류·태그 이름 포함). 대분류 409 `QUESTION_NEEDS_SUBCATEGORY`, 숨긴 카테고리 409 `CATEGORY_HIDDEN`, 없는 카테고리 404, 없는 태그 400, 직무·태그 목록에 null이 섞이면 400. 저장하면 임베딩도 만든다(키가 없거나 외부 호출이 실패해도 질문은 저장하고 임베딩만 비운 뒤 기동 시 채운다) |
| PUT | `/api/admin/questions/{id}` | ADMIN | 같은 본문으로 전체 수정(임베딩 다시 만듦). 지금 있는 숨긴 카테고리 안에서는 고칠 수 있지만 다른 숨긴 카테고리로는 못 옮김 |
| PATCH | `/api/admin/questions/{id}/active` | ADMIN | `{active}` 숨기기/다시 보이기(삭제 없음) |
