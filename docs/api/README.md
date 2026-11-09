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
| GET | `/api/admin/questions/{id}` | ADMIN | 질문 하나(수정 화면용, 숨긴 질문도). 없으면 404 |
| POST | `/api/admin/questions` | ADMIN | `{categoryId, text(5~300), level, jobRoles[1+], tagIds[≤10], intent?(≤200)}` → 201 `QuestionView`(대분류·소분류·태그 이름 포함). 대분류 409 `QUESTION_NEEDS_SUBCATEGORY`, 숨긴 카테고리 409 `CATEGORY_HIDDEN`, 없는 카테고리 404, 없는 태그 400, 직무·태그 목록에 null이 섞이면 400. 저장하면 임베딩도 만든다(키가 없거나 외부 호출이 실패해도 질문은 저장하고 임베딩만 비운 뒤 기동 시 채운다) |
| PUT | `/api/admin/questions/{id}` | ADMIN | 같은 본문으로 전체 수정(임베딩 다시 만듦). 지금 있는 숨긴 카테고리 안에서는 고칠 수 있지만 다른 숨긴 카테고리로는 못 옮김 |
| PATCH | `/api/admin/questions/{id}/active` | ADMIN | `{active}` 숨기기/다시 보이기(삭제 없음) |
| POST | `/api/admin/questions/import` | ADMIN | CSV 대량 등록(multipart `file`, UTF-8, 2MB, 1000줄). 전부 검사한 뒤 한 번에 저장하고, 오류가 하나라도 있으면 0건 저장 → 200 `{imported, errors[{line, message}]}`. 2MB 초과·UTF-8 아님은 400 |


### 면접 (`/api/interviews`, 로그인 사용자 · 본인 것만, 남의 것은 404)
| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/interviews` | `{type, count(3·5·7), level?, jobRole?, resumeId?, experienceId?, narrowByResume?}` → 201 `InterviewView`(질문 포함, 의도는 숨김). 기술·혼합은 직무·연차 필수(409 `CONDITIONS_REQUIRED`), 질문 수 409 `INVALID_QUESTION_COUNT`, 질문 은행이 모자라면 409 `NOT_ENOUGH_QUESTIONS`, 남의 이력서로 좁히면 404. 경험 기반(`EXPERIENCE`)은 `resumeId`·`experienceId` 필수(남의 것이거나 그 이력서의 경험이 아니면 404), 질문은 AI가 만들고 실패하면 질문 은행 후보로 대신 |
| GET | `/api/interviews` | 내 면접 목록(최신순) `InterviewSummary[]` |
| GET | `/api/interviews/{id}` | 면접 하나(질문 포함, 질문마다 `answerStatus`·`failureReason`) |
| POST | `/api/interviews/{id}/questions/{seq}/answer` | multipart `audio`(audio/*, 10MB) → 202 `{seq, status}`. 같은 질문에 다시 올리면 다시 녹음(글자·평가를 지우고 처음부터). 백그라운드 STT → 평가(혼합은 STT까지, 평가는 끝낸 뒤). 끝난 면접 409 `INTERVIEW_CLOSED`, 없는 질문 404, 오디오 아님·빈 파일 400 |
| POST | `/api/interviews/{id}/questions/{seq}/retry` | 실패한 답변만(409 `NOT_FAILED`) 같은 녹음으로 다시 처리 → 202 |
| GET | `/api/interviews/{id}/questions/{seq}/audio` | 내 녹음 듣기(원래 Content-Type, `no-store`). 남의 것·답하지 않은 질문 404 |
- 질문 고르기: 기술은 인성이 아닌 대분류에서 직무(또는 COMMON)·연차(같은 단계, 모자라면 한 단계 아래), 이력서로 좁히면 이력서 기술과 맞는 태그·소분류 먼저, 소분류가 겹치지 않게. 인성은 인성 대분류. 혼합은 기술·인성을 기술부터 번갈아.

### 질문 CSV 형식 (`POST /api/admin/questions/import`)
```
대분류,소분류,질문,난이도,직무,태그,의도
CS,네트워크,TCP와 UDP의 차이를 설명해 주세요.,신입,백엔드|공통,TCP|UDP,기본기 확인
Framework,Spring,"JPA에서 N+1 문제가 생기는 이유와, 해결 방법을 설명해 주세요.",주니어,BACKEND,JPA,
```
- 첫 줄(헤더)은 위와 똑같아야 한다. UTF-8(BOM 있어도 됨). 엑셀에서는 "CSV UTF-8"로 저장한다.
- 쉼표·줄바꿈이 들어간 칸은 큰따옴표로 감싼다(`""`는 따옴표 한 개). 빈 줄은 건너뛴다.
- 대분류·소분류는 이미 있는 이름(공백·대소문자 무시). 없거나 숨긴 카테고리는 오류다(오타로 카테고리가 생기지 않게).
- 난이도: 신입·주니어·미들·시니어 또는 NEW·JUNIOR·MIDDLE·SENIOR
- 직무(1개 이상, `|`로 구분): 백엔드·프론트엔드·풀스택·모바일·데이터·DevOps·공통 또는 영문 코드
- 태그(선택, `|`, 최대 10개): 없으면 새로 만들고, 있으면 그 태그를 쓴다(대소문자 무시)
- 의도(선택): 200자까지
- 형식 오류와 카테고리 오류를 한 번에 줄 순서로 알려준다(행마다 첫 번째 하나). line은 파일의 줄 번호(헤더 = 1, 여러 줄 칸은 그 행이 시작한 줄)
