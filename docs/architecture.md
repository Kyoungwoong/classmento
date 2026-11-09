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

## 백엔드 구조 — DDD (규칙: [conventions/ddd-tdd.md](conventions/ddd-tdd.md))
바운디드 컨텍스트 × 계층. 의존 방향 `presentation → application → domain ← infrastructure`, 컨텍스트끼리 직접 의존 금지. `ArchitectureTest`(ArchUnit)가 강제한다.

| 컨텍스트 | domain | application | infrastructure | presentation |
|---|---|---|---|---|
| `identity` 회원 | `AppUser`, `Email`(값 객체), `AppUserRepository` | `SignupService`, `MemberQueryService`, `AdminSeeder` | `persistence.AppUserJpaRepository`, `security.*`(SecurityConfig, 소셜, UserDetails, `AuthUsers`) | `AuthController`, `AdminController` |
| `resume` 이력서 | `Resume`(상태 전이), `ResumeExperience`, `ExperienceFactory`, `ParsedResume`, `ResumeFileType`, `TextChunker`, `ResumeRepository`, `ResumeChunkRepository`, `ResumeExperienceRepository` | `ResumeService`, `ResumeProcessor`, `ResumeParser`, `ExperienceSync`, `TextExtractor`(포트) | `ResumeJpaRepository`, `JdbcResumeChunkRepository`·`JdbcResumeExperienceRepository`(pgvector), `PdfDocxTextExtractor`, 실행기 | `ResumeController` |
| `questionbank` 질문 은행 | `Category`(2단계·숨김), `Tag`, `Question`(소분류만·직무·태그 ID), `QuestionEmbeddingText`, `QuestionCsv`(CSV 형식·줄별 오류), 포트 `CategoryRepository`·`TagRepository`·`QuestionRepository`·`QuestionSearchRepository`·`QuestionEmbeddingRepository` | `CategoryService`, `TagService`, `QuestionService`(임베딩 prepare → 트랜잭션 save), `QuestionEmbeddingBackfill`(기동 시 빈 임베딩 채우기), `QuestionImportService`(CSV 전부 아니면 0건) | `*JpaRepository`, `JdbcQuestionSearchRepository`(필터 조합), `JdbcQuestionEmbeddingRepository`(전역 kNN, HNSW) | `AdminCategoryController`, `AdminTagController`, `AdminQuestionController` |
| `interview` 면접 | `InterviewSession`(탭·조건·질문 3/5/7, 상태), `InterviewQuestion`, 포트 `QuestionSource`·`ExperienceSource`·`InterviewSessionRepository` | `InterviewService`(탭별 질문 고르기·혼합 번갈아) | `InterviewSessionJpaRepository`, 어댑터 `QuestionBankAdapter`(→ questionbank `QuestionCatalog`)·`ResumeAdapter`(→ resume `ExperienceQuery`) | `InterviewController` |

| 공용 · 플랫폼 | 내용 |
|---|---|
| `common` (공용 커널) | `web`(ApiError/ApiException/GlobalExceptionHandler/CORS/Health), `security.AuthUser`(현재 사용자), `domain.DomainException`, `AiProperties` |
| `llm` | `LlmClient` 포트 / `ClaudeLlmClient` / `DevFakeLlmClient` / `PromptLoader` |
| `embedding` | `EmbeddingClient` 포트 / `OpenAiEmbeddingClient` / `HashEmbeddingClient`(가짜) / `PgVectors` |
| `speech` | `SttClient` 포트 / `ClovaSttClient` |
| `storage` | `FileStorage` 포트 / `LocalFileStorage` (나중에 S3) |

규칙
- **도메인 규칙은 애그리거트 안에 둔다.**
  - 예: `Resume`의 상태 전이(UPLOADED → EXTRACTED → PARSED, 처리 중 FAILED), `Email` 형식.
  - 위반하면 `DomainException`을 던지고, 응답은 409 + `code`다.
- 외부 SDK·HTTP 코드는 플랫폼 어댑터나 각 컨텍스트의 infrastructure 안에만 둔다. application은 포트 인터페이스만 쓴다.
- 어댑터 선택 우선순위: **실제 키 → (`classmento.ai.fake=true`일 때) 가짜 → `Unavailable*`(503 `AI_UNAVAILABLE`)**.
- 현재 사용자: 컨트롤러에서 `@AuthenticationPrincipal AuthUser user`로 받는다(공용 커널). 다른 컨텍스트는 `user.id()`만 쓴다.
- 남의 리소스는 `findByIdAndUserId`로 조회하고, 없으면 404를 준다(존재 여부를 숨긴다).

## RAG 흐름
```
[이력서]  업로드 → 텍스트 추출 → 청크(~800자) → 임베딩 → resume_chunk          (전체 검색용)
          → Claude 구조화(v2) → ExperienceFactory → 경험마다 임베딩 → resume_experience (경험 단위)
[면접, ADR 0005]
  EXPERIENCE : 사용자가 고른 경험(제목·요약·본문·기술) → 경험 벡터로 question_bank kNN + Claude 질문 생성
  TECH       : question_bank(Language·Framework·CS) 필터(+이력서 기술) → 선택·변형
  BEHAVIORAL : question_bank(인성)
  MIXED      : TECH + BEHAVIORAL
```
- 거리: `<=>` 코사인 거리.
- 인덱스 정책:
  - **이력서별 검색**(`resume_chunk`, `resume_experience`, `WHERE resume_id = ?`)은 HNSW를 쓰지 않고 정확 정렬한다(V3). `resume_id`로 좁히는 데는 유니크 제약 `(resume_id, seq)`가 만든 인덱스를 쓴다(V4에서 중복 인덱스 제거). 전역 HNSW에 필터를 걸면 후보 선필터 때문에 결과가 k개보다 적어질 수 있어서다.
  - **전역 검색**(질문 은행 등)에만 HNSW `vector_cosine_ops`를 쓴다.
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
resume_experience(id, resume_id, seq, type[WORK|PROJECT], title, organization, period, summary, detail, skills jsonb, embedding vector(1536))
spring_session, spring_session_attributes      -- 로그인 세션
```
예정:
```
qb_category(id, parent_id?, name, sort_order, active, seeded)   -- V5, 대분류 → 소분류 2단계, 형제 이름 유니크(공백·대소문자 무시)
qb_tag(id, name, name_key UNIQUE, seeded)                         -- V5
qb_question(id, category_id → 소분류, text, level, intent?, active, seeded, created_by?, embedding vector(1536)?)  -- V6, HNSW(전역 kNN)
qb_question_job_role(question_id, job_role) · qb_question_tag(question_id, tag_id)                                  -- V6
  임베딩 문장: "대분류 > 소분류 · 질문 · 태그(이름순)". 시드는 비워 두고 기동 시 채운다
interview_session(id, user_id, type[EXPERIENCE|TECH|BEHAVIORAL|MIXED], level?, job_role?, resume_id?, experience_id?, status)  -- V7, 탭이 쓰는 조건만 저장
interview_question(session_id, seq, text, intent?, kind[TECH|BEHAVIORAL], bank_question_id?, generated)              -- V7
interview_answer(session_id, user_id, seq UNIQUE/세션, kind, audio_key, content_type, audio_version, status[UPLOADED|TRANSCRIBED|EVALUATED|FAILED], transcript?, failure_reason?, overall_score?, strength?, improvement?, suggestion?, prompt_version?, model?)  -- V8
interview_answer_score(answer_id, criterion, score 1~5, evidence?)                                                    -- V8, 루브릭 기준별
job_posting(id, user_id, text, embedding)
credit_ledger / payment  → ADR 0003
```
스키마 변경은 **새 Flyway 파일**(`V2__...sql`)로만 한다. 이미 적용된 파일은 수정하지 않는다.

## 설정 / 비밀값
루트 `.env.example`을 참고한다. 로컬에서는 셸 환경변수나 IDE 실행 설정으로 주입한다. `.env` 파일은 커밋하지 않는다.
로컬 기본값: DB `localhost:5433` classmento/classmento, 관리자 `admin@classmento.local` / `admin1234!` (local 프로필 전용).
