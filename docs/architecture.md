# 아키텍처

```
[Browser: Next.js]  ──REST(JSON, multipart)──▶  [Spring Boot API]  ──▶ Claude Messages API (질문 생성·이력서 구조화·평가)
   녹음(MediaRecorder)                              │                 ──▶ CLOVA Speech (STT, ko-KR)
                                                    └──▶ DB (local: H2 파일 / prod: PostgreSQL)
```

- 프론트엔드는 **화면과 녹음만** 담당한다. AI 키는 절대 브라우저에 두지 않는다. 모든 AI 호출은 백엔드에서.
- 계약(API 스펙)의 원본은 백엔드가 생성하는 OpenAPI(`/v3/api-docs`). 프론트 타입은 여기서 생성 → [api/README.md](api/README.md)

## 백엔드 패키지 (`backend/src/main/java/com/classmento`)
| 패키지 | 책임 | 의존 가능 |
|---|---|---|
| `common` | 웹 설정(CORS), 공통 에러(`ApiError`, `GlobalExceptionHandler`), 헬스 | — |
| `llm` | `LlmClient` 포트, `ClaudeLlmClient` 어댑터, `PromptLoader` | common |
| `speech` | `SttClient` 포트, `ClovaSttClient` 어댑터 | common |
| `resume` | 업로드·텍스트 추출·구조화 | common, llm |
| `interview` | 세션·질문 생성·답변 수신 | common, llm, speech, resume |
| `evaluation` | 답변 채점·리포트 | common, llm, interview |

규칙
- 도메인 패키지는 `LlmClient`/`SttClient` **인터페이스만** 의존한다. Anthropic SDK·CLOVA HTTP 코드는 각 어댑터 안에만.
- 도메인 패키지 내부 구조: `XxxController`(web) → `XxxService`(유스케이스) → `XxxRepository`(JPA). DTO는 `record`.
- 키가 없으면 `Unavailable*Client`가 등록되어 앱은 뜨고, AI 호출 시 503 `AI_UNAVAILABLE`.

## 외부 연동
### Claude
- SDK: `com.anthropic:anthropic-java`. 모델 기본값 `claude-sonnet-5-5` (`CLASSMENTO_LLM_MODEL`로 변경).
- 프롬프트: `backend/src/main/resources/prompts/<name>.v<N>.md` → [ai/interview-and-rubric.md](ai/interview-and-rubric.md)
- 출력은 JSON으로 받고 서버에서 파싱·검증한다(파싱 실패 시 1회 재시도).

### CLOVA Speech
- 장문 인식 API: `POST {CLOVA_SPEECH_INVOKE_URL}/recognizer/upload`, 헤더 `X-CLOVASpeech-API-Key`, multipart(`media`, `params`), `completion=sync`, `language=ko-KR`.
- 브라우저 녹음은 `audio/webm`(Chrome) 또는 `audio/mp4`(Safari). 지원 포맷 확인 후 필요하면 서버에서 변환.

## 데이터 모델 초안
```
Resume(id, originalFilename, extractedText, parsedJson, createdAt)
InterviewSession(id, resumeId, type, status, createdAt)
Question(id, sessionId, seq, text, intent, source)
Answer(id, questionId, audioPath, transcript, durationMillis, createdAt)
Evaluation(id, answerId, scoresJson, strength, improvement, suggestion, model, promptVersion)
```
`Evaluation`에는 사용한 모델과 프롬프트 버전을 반드시 저장한다(평가 회귀 추적용).

## 설정 / 비밀값
루트 `.env.example` 참고. 로컬에서는 셸 환경변수 또는 IDE 실행 설정으로 주입. `.env` 파일은 커밋 금지.
