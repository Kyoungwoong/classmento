# 아키텍처

```
[Browser] ──/api, /oauth2──▶ [Next.js :3000]  ──rewrites(프록시)──▶ [Spring Boot :8080] ──▶ Claude Messages API (구조화·질문·평가)
  녹음(MediaRecorder)          화면 + proxy.ts(로그인 체크)                │                ──▶ OpenAI Embeddings (벡터)
  세션 쿠키(HttpOnly)                                                     │                ──▶ CLOVA Speech (STT, ko-KR)
                                                                          └──▶ PostgreSQL 17 + pgvector (:5433, WSL Docker)
```

- 프론트엔드는 **화면과 녹음만** 담당한다. AI 키는 절대 브라우저에 두지 않는다. 모든 AI 호출은 백엔드에서 한다.
- 브라우저는 항상 같은 출처(`localhost:3000/api/...`)를 부르고, Next가 백엔드로 넘긴다. 쿠키·CSRF·CORS가 단순해진다.
- 계약(API 스펙)의 원본은 백엔드가 생성하는 OpenAPI(`/v3/api-docs`)다. 프론트 타입은 여기서 생성한다 → [api/README.md](api/README.md)
- 결정 기록: [ADR 0002](adr/0002-postgres-pgvector-auth.md)(DB·인증·임베딩), [ADR 0003](adr/0003-credits.md)(크레딧 설계)

## 백엔드 패키지 (`backend/src/main/java/com/classmento`)
| 패키지 | 책임 | 의존 가능 |
|---|---|---|
| `common` | CORS, `ApiError`/`ApiException`/`GlobalExceptionHandler`, `AiProperties`, 헬스 | — |
| `auth` | 회원(`AppUser`), 로그인·가입·세션(`SecurityConfig`), 소셜(`SocialUserService`), 관리자 시드 | common |
| `admin` | 관리자 전용 API (`/api/admin/**`, ROLE_ADMIN) | common, auth |
| `llm` | `LlmClient` 포트 / `ClaudeLlmClient` / `DevFakeLlmClient` / `PromptLoader` | common |
| `embedding` | `EmbeddingClient` 포트 / `OpenAiEmbeddingClient` / `HashEmbeddingClient`(가짜) / `PgVectors` | common |
| `speech` | `SttClient` 포트 / `ClovaSttClient` | common |
| `storage` | `FileStorage` 포트 / `LocalFileStorage` (나중에 S3) | — |
| `resume` | 업로드 → 추출(PDFBox/POI) → 청크·임베딩 → 구조화, kNN 검색 | common, auth, llm, embedding, storage |
| `questionbank` | 질문 은행 관리(관리자 CRUD + 임베딩) · 조건/kNN 검색 (예정) | common, embedding |
| `interview` | 세션(4가지 유형, ADR 0004)·질문 생성·꼬리질문·답변 수신 (예정) | common, llm, speech, resume, questionbank, embedding |
| `evaluation` | 답변 채점·리포트 (예정) | common, llm, interview |

규칙
- 도메인 패키지는 포트 **인터페이스만** 의존한다(`LlmClient`, `EmbeddingClient`, `SttClient`, `FileStorage`). 외부 SDK·HTTP 코드는 어댑터 안에만 둔다.
- 도메인 패키지 내부 구조: `XxxController` → `XxxService` → `XxxRepository`(JPA, 벡터는 JdbcTemplate). DTO는 `record`.
- 어댑터 선택 우선순위: **실제 키 → (`classmento.ai.fake=true`일 때) 가짜 → `Unavailable*`(503 `AI_UNAVAILABLE`)**.
- 현재 사용자: 컨트롤러에서 `@AuthenticationPrincipal AuthUser user`. 남의 리소스는 `findByIdAndUserId`로 조회하고, 없으면 404를 준다(존재 여부를 숨김).

## RAG 흐름
```
[이력서]  업로드 → 텍스트 추출 → 청크(문단 기준 ~800자, 겹침 100자) → 임베딩 → resume_chunk(vector(1536))
[질문 생성 시, 유형별]
  TECH        : 조건(직무·연차·스택·CS) 필터 + 조건 문장 임베딩 → question_bank kNN → 후보를 프롬프트에 → Claude
  RESUME_TECH : 이력서 기술·프로젝트 → resume_chunk kNN + question_bank(태그 일치) → Claude
  DEEP_DIVE   : 주제(이력서 청크 또는 기술) + 지금까지의 Q/A → Claude가 다음 질문 1개(또는 done)
  BEHAVIORAL  : question_bank(category=BEHAVIORAL)에서 카테고리 안 겹치게 선택 → Claude는 말투만 다듬음
```
- 거리: `<=>` 코사인 거리. 인덱스: HNSW `vector_cosine_ops`.
- 가짜 임베딩(`HashEmbeddingClient`)은 단어 해시 기반이다. 같은 단어를 공유하면 가깝게 나오지만, 의미 검색 품질은 실제 키로만 확인할 수 있다.

## 이력서 처리 상태
```
POST /api/resumes → 202 (UPLOADED)
  resumeExecutor(백그라운드): 추출·청크·임베딩 → EXTRACTED → Claude resume-parse.v1 → PARSED
                              실패 시 → FAILED + failureReason(사용자용 문장)
화면은 GET /api/resumes/{id}를 1.5초마다 폴링한다.
```

## 외부 연동
### Claude
- SDK: `com.anthropic:anthropic-java`. 모델 기본값 `claude-sonnet-5-5` (`CLASSMENTO_LLM_MODEL`로 변경).
- 프롬프트: `backend/src/main/resources/prompts/<name>.v<N>.md` → [ai/interview-and-rubric.md](ai/interview-and-rubric.md). 가짜 응답: `resources/fake/<name>.json`.
- 출력은 JSON으로 받고 서버에서 파싱·검증한다(파싱 실패 시 1회 재시도).

### OpenAI Embeddings
- `POST https://api.openai.com/v1/embeddings` (`text-embedding-3-small`, `dimensions=1536`). RestClient로 직접 호출한다.

### CLOVA Speech
- 장문 인식 API: `POST {CLOVA_SPEECH_INVOKE_URL}/recognizer/upload`
  - 헤더 `X-CLOVASpeech-API-Key`, multipart(`media`, `params`), `completion=sync`, `language=ko-KR`
- 브라우저 녹음은 `audio/webm`(Chrome) 또는 `audio/mp4`(Safari)다. 지원 포맷을 확인하고, 필요하면 서버에서 변환한다.

## 데이터 모델
현재 스키마: `backend/src/main/resources/db/migration/V1__init.sql`
```
app_user(id, email, password_hash?, name, role[USER|ADMIN], provider[LOCAL|GOOGLE|KAKAO], provider_id?, created_at)
resume(id, user_id, original_filename, content_type, size_bytes, storage_key, status, failure_reason,
       extracted_text, parsed_json(jsonb), parse_model, prompt_version, created_at, updated_at)
resume_chunk(id, resume_id, seq, content, embedding vector(1536))
spring_session, spring_session_attributes      -- 로그인 세션
```
예정:
```
question_bank(id, category[CS|STACK|ROLE|BEHAVIORAL], job_role, level, tags[], text, intent, eval_points, embedding, created_by)
interview_session(id, user_id, mode[TECH|RESUME_TECH|DEEP_DIVE|BEHAVIORAL], resume_id?, config(jsonb), status)
question(id, session_id, seq, parent_question_id?, depth, text, intent, source)
answer(id, question_id, audio_key, transcript, duration_ms) / evaluation(..., model, prompt_version)
job_posting(id, user_id, text, embedding)
credit_ledger / payment  → ADR 0003
```
스키마 변경은 **새 Flyway 파일**(`V2__...sql`)로만 한다. 이미 적용된 파일은 수정하지 않는다.

## 설정 / 비밀값
루트 `.env.example`을 참고한다. 로컬에서는 셸 환경변수나 IDE 실행 설정으로 주입한다. `.env` 파일은 커밋하지 않는다.
로컬 기본값: DB `localhost:5433` classmento/classmento, 관리자 `admin@classmento.local` / `admin1234!` (local 프로필 전용).
