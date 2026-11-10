# 꼬리질문 — 답을 듣고 AI가 한 번 더 파고들기

진행: [x] F1 도메인·DB · [x] F2 생성 · [x] F3 API·계약 · [x] F4 화면

ADR: [0008](../../adr/0008-follow-up-questions.md)


## Context
- 로드맵(`docs/product/overview.md`)에서 질문 은행, 면접, 녹음, STT, 평가, 리포트까지 끝났다. 다음 기능은 꼬리질문이다.
- 지금은 면접을 만들 때 질문을 모두 정한다. 질문에는 부모나 깊이 같은 필드가 없다(`InterviewSession`, `InterviewQuestion`, V7 주석 "꼬리질문 없음").
- ADR 0005는 "꼬리질문을 경험 기반 안에 넣을지는 면접 기능을 만들 때 정한다"로 결정을 미뤄 두었다.

사용자와 정한 것
- 새 면접의 **옵션**으로 둔다. 질문 하나에 꼬리질문을 **없음 / 한 번 / 두 번**(최대) 중에서 고르고, 기본값은 한 번이다.
- **인성은 제외**한다.
  - 경험 기반, 기술, 혼합 면접에서 쓸 수 있다. 혼합은 기술 질문에만 붙는다.
  - 인성 탭에서는 옵션을 보이지 않는다.
- AI가 답이 충분하다고 보면(`done`) 최대 횟수보다 일찍 멈춘다.

목표
- 답변을 올린 뒤 꼬리질문을 받기까지 **8초 안**(ADR 0004)에 끝낸다. 넘으면 "다음 질문을 고르고 있어요"를 보여 준다.

## 설계

### 도메인 (`interview/domain`)
- **`InterviewQuestion`에 필드 추가**: `parentSeq`(null이면 본 질문), `rootSeq`, `depth`(본 질문은 0), `followUp`(`FollowUpState`)
  - 팩토리: `of(seq, draft, eligible)`, `followUpOf(seq, parent, draft)`
- **`FollowUpState`**: `NONE`(대상 아님) / `AWAITING`(아직 판단 안 함) / `ASKED`(꼬리를 붙임) / `CLOSED`(AI done·실패·짧은 답)
  - "생성 중"은 따로 저장하지 않고 계산한다: AWAITING이면서 답변이 있으면 생성 중이다.
- **`InterviewSession`**
  - `maxFollowUps`(0~2)를 둔다. `start(..., maxFollowUps, drafts)`로 받는다.
  - `requireValid`에 오류를 더한다: `INVALID_FOLLOW_UPS`, 인성 면접인데 0이 아니면 `FOLLOW_UPS_NOT_ALLOWED`.
  - 꼬리 대상 조건: `maxFollowUps > 0 && kind == TECH && depth < maxFollowUps`
  - `addFollowUp(parentSeq, draft)`
    - 세션이 진행 중이고 부모가 AWAITING이며 같은 문장이 없을 때만 받는다.
    - seq는 지금 가장 큰 seq + 1이다.
    - 부모는 ASKED, 새 꼬리는 AWAITING 또는 NONE이 된다. 다시 부르면 `FOLLOW_UP_DECIDED`.
  - `closeFollowUp(seq)`: AWAITING일 때만 CLOSED로 바꾼다. 아니면 아무 일도 하지 않는다(같은 결과가 와도 안전).
  - `awaitingFollowUps(answers)`
  - `finish`: AWAITING이 남아 있으면 `FOLLOW_UP_PENDING`.
  - `requireReRecordable(seq)`: 부모가 ASKED면 `FOLLOW_UP_ASKED`(409).
  - `getQuestions()`는 (rootSeq, depth) 순서로 돌려준다. `rootCount()`도 둔다.
- seq는 계속 고정 식별자다. 답변의 unique 키, 질문 PK, URL을 그대로 쓴다.

### 생성 (`interview/application`)
- **`AnswerProcessor.run`**: STT 성공 → `FollowUpService.decide(answerId, version)` → 평가 순서로 바꾼다. 꼬리를 평가보다 먼저 정한다.
- **`FollowUpService.decide`**
  1. 미리 거른다: 버전·상태, 세션이 진행 중, 질문이 AWAITING인지
  2. 짧은 답(30자 미만, "모르겠습니다"류)이면 LLM을 부르지 않고 CLOSED
  3. 질문·intent·답 사슬을 만든다
  4. 트랜잭션 밖에서 `FollowUpWriter`를 부른다
  5. `sessions.lockById` → `answers.lockById` 순서로 잠근다(`record`와 같은 순서). 다시 확인한 뒤 `addFollowUp` 또는 `closeFollowUp`
  6. 예외를 밖으로 내지 않는다. 실패하면 CLOSED로 두고 평가를 계속한다. 꼬리 때문에 답변이 FAILED가 되지 않게 한다.
- **`FollowUpWriter`**
  - 프롬프트 `follow-up-question.v1`, 출력 `{done, basis, question, intent}`
  - 서버 검증
    - `basis`가 마지막 답변 안에 실제로 있어야 한다. 환각 방지의 핵심이다.
    - 질문 5~300자, intent 200자 이하
    - 앞에 나온 질문과 같은 문장이면 안 된다
  - 시간 제한 5초. JSON이 틀려도 다시 부르지 않는다(8초 목표). 출력 토큰은 짧게 잡는다.
  - 입력은 사슬과 직무·연차만 넣는다. 이력서 원문은 넣지 않는다. 답변의 꺾쇠는 바꾼다(`AnswerEvaluator.escape`를 다시 쓴다).
- **`FollowUpScheduler`**(`ReportScheduler` 패턴: inFlight/pending)
  - AWAITING이 남아 멈춘 세션을 복구한다.
  - `GET /api/interviews/{id}`를 부르거나 `finish`가 `FOLLOW_UP_PENDING`일 때 다시 판단하게 한다.
- **실행기 분리**: `InterviewExecutorConfig`에 `answerFastExecutor`를 둔다(STT와 꼬리, core 4).
  - 평가는 기존 `interviewExecutor`로 넘긴다. 평가가 다음 질문을 밀어내지 않게 하려는 것이다.
  - 테스트(`sync-processing`)에서는 둘 다 동기로 돈다.
- 측정용으로 STT부터 꼬리 결정까지 걸린 시간을 로그로 남긴다.

### 다시 쓰는 것
- `ExperienceQuestionWriter`: 프롬프트 호출, JSON 파싱, 대체 처리 패턴
- `stripFence`, `PromptLoader`, `AnswerEvaluator`의 꺾쇠 치환
- `ReportScheduler`: 세션별 1개 실행 패턴
- `AnswerProcessor.save`: 잠금과 버전 확인
- 테스트 가짜: `FakeLlmClient`, `ScriptedLlm`, `IntegrationTest`

### DB, API (V10)
- `interview_session.max_follow_ups INT NOT NULL DEFAULT 0`
- `interview_question`에 `parent_seq`, `root_seq`(기존 행은 seq로 채움, NOT NULL), `depth INT NOT NULL DEFAULT 0`, `follow_up VARCHAR(20) NOT NULL DEFAULT 'NONE'`을 더한다.
  - 기존 면접은 그대로 동작한다.
- `CreateInterviewRequest.followUps`(없으면 0, 서버 기본 0)
- `InterviewView`에 `maxFollowUps`를 더한다.
- `InterviewQuestionView`에 `parentSeq`, `depth`, `followUp`, `retryable`(글자가 있어 평가만 다시 하면 되는 실패)을 더한다.
- `ReportItemView`에 `parentSeq`, `depth`를 더한다.
- `InterviewSummary.questionCount`는 본 질문 수(`rootCount`)로 바꾼다.
- 오류 코드 `FOLLOW_UP_PENDING`, `FOLLOW_UP_ASKED`를 `docs/api/README.md`에 적는다. 계약을 바꾼 뒤 `pnpm gen:api`를 돌린다.

### 리포트
- 평균은 답변 전체(꼬리 포함)로 같은 비중으로 계산한다.
- 문항 목록에서 꼬리는 부모 아래에 들여 쓴다.
- 일관성(consistency) 기준은 다음 버전으로 미룬다.

### 프론트
- **새 면접**(`NewInterviewScreen`): 마지막 단계에 `SegmentedControl` "꼬리질문: 없음 / 한 번 / 두 번"(기본 한 번)을 둔다. 인성이면 숨기고 0을 보낸다.
- **진행 화면**(`InterviewScreen`)
  - 업로드한 질문이 AWAITING이면 1초마다 다시 불러온다.
    - 마스코트 thinking과 "다음 질문을 고르고 있어요"를 보여 준다.
    - 질문이 AWAITING에서 벗어나면 `firstOpen`으로 다음 질문을 정한다. 꼬리가 붙었으면 꼬리로, 아니면 다음 본 질문으로 간다.
    - 그 사이 답변이 FAILED가 되면 그 질문으로 돌아간다. 20초가 넘으면 안내한다.
  - 지금 질문이 꼬리 대상이면 마지막 질문이 아니다(`isLast`).
  - 진행 표시는 본 질문 기준 `i / N`이다. 꼬리는 노트에 "질문 2의 꼬리 1"과 부모 질문 한 줄로 보여 준다.
  - `retryable`인 실패는 재녹음 대신 [다시 평가하기](`retryAnswer`)를 보여 준다. 지금도 진행 중 평가 실패에 재녹음만 시키는 틈이 있는데, 이것도 함께 고친다.
  - `finish`가 `FOLLOW_UP_PENDING`이면 다시 불러온다.
- **리포트**(`ReportScreen`, `ReportItemScreen`): 꼬리를 들여 쓰고 "질문 2의 꼬리 1" 라벨을 붙인다. 문항 상세에서 부모 질문으로 갈 수 있게 한다.

### 정해 둔 기본값(결정 로그에 적는다)
- CLOSED로 끝난 본 질문은 다시 녹음해도 꼬리를 다시 판단하지 않는다(단순하게).
- 짧은 답 기준은 공백을 빼고 30자 미만이거나 "모르겠" 같은 문구가 들어간 경우다.

## 문서
- **ADR 0008**: 꼬리질문을 옵션으로 두고 인성은 제외한다. 0005의 미룬 결정을 대체한다.
- `docs/ai/interview-and-rubric.md`
  - "꼬리질문 없음" 문구를 고친다.
  - 프롬프트 절을 추가한다(`interview-ai` 스킬 절차).
  - `fake/follow-up-question.v1.json`을 만든다. `basis`는 `DevFakeSttClient` 문장 안의 구절이어야 한다.
- `docs/design/screens.md` 4·5·6절
- 제품 개요(`overview.md`)의 용어(DEEP_DIVE 등)를 지금 구조에 맞게 고치고, "다음" 단계를 갱신한다.
- 끝난 실행계획 3개(면접 세션, 질문 은행, 디자인 개편)를 `completed/`로 옮긴다.
- 실행계획은 `docs/exec-plans/active/2026-11-10-follow-up-questions.md`(이 내용)에 둔다.

## PR 나누기
매번 같은 루프를 돈다: 브랜치 → red/green 커밋 → verify → PR → pr-reviewer → 머지.

| PR | 브랜치 | 내용 |
|---|---|---|
| F1 | `feat/follow-up-domain` | V10, 도메인(상태·추가·닫기·finish·재녹음 막기·정렬·rootCount), ADR 0008, 계획 파일, 끝난 계획 옮기기. 옵션이 0이면 동작은 지금과 같다 |
| F2 | `feat/follow-up-generation` | 프롬프트 v1과 가짜 응답, `FollowUpWriter`, `FollowUpService`, `FollowUpScheduler`, 실행기 분리, 시간 제한, 복구 |
| F3 | `feat/follow-up-api` | 요청·응답 필드, 오류 코드, `retryable`, OpenAPI → `pnpm gen:api`, `docs/api` |
| F4 | `feat/follow-up-ui` | 새 면접 옵션, 진행 화면 폴링과 꼬리 표시, 다시 평가하기, 리포트 들여쓰기, screens.md |

## Verification
- **도메인 테스트**(`InterviewSessionTest` 등)
  - followUps 범위와 인성 거절, 혼합은 TECH만, 깊이 한도
  - `addFollowUp`의 순번, 깊이, 같은 일을 두 번 할 때
  - `finish`의 `FOLLOW_UP_PENDING`과 CLOSED 통과
  - 정렬
- **`FollowUpWriter` 테스트**(`ScriptedLlm`): done, `basis`가 답에 없음, 길이, 같은 문장, JSON 깨짐, 사용 불가, 시간 초과. 모두 CLOSED가 나와야 한다.
- **`FollowUpService` 테스트**: 버전이 바뀐 결과 버리기, 끝난 면접 건너뛰기, 짧은 답이면 LLM을 부르지 않기
- **통합 테스트**(:5433, 가짜 AI, 동기)
  - 업로드 → 꼬리 → 꼬리의 꼬리 → 끝내기 → 리포트
  - 혼합에서 평가를 미룬 채 꼬리 생성
  - 재녹음 409, GET 복구
  - `@ElementCollection` 전체를 다시 써도 행이 빠지지 않는지
  - 생성 도중 재녹음(`CountDownLatch`)
- **프론트(vitest)**: 폴링 뒤 꼬리로 이동, 꼬리 없으면 다음 본 질문, 기다리는 중 실패, `isLast`, 인성 옵션 숨김, 다시 평가하기
- **`bash scripts/verify.sh`** 통과(BE와 FE)
- **브라우저(가짜 AI)**: 기술 면접 3개, 꼬리 두 번 → 녹음 → 꼬리 화면 → 끝내기 → 리포트 들여쓰기
  - 375 / 768 / 1280 스크린샷을 찍고 design-reviewer로 확인한다.
  - 가로 넘침 검사를 한다. 끝나면 테스트 브라우저를 닫는다.
- **8초 목표**: 로그에 남긴 STT부터 꼬리 결정까지의 시간으로 확인한다. 실제 키로 재는 것은 사용자가 키를 넣은 뒤에 한다.

## 결정 로그
- 2026-11-10 위치: 새 면접 옵션(없음/한 번/두 번, 기본 한 번), 인성 제외(혼합은 기술 질문만) — 사용자 결정
- 2026-11-10 상태는 질문에 둔다(FollowUpState NONE/AWAITING/ASKED/CLOSED), 생성 중은 계산
- 2026-11-10 CLOSED 본 질문은 재녹음해도 다시 판단하지 않음, 짧은 답 기준 30자·"모르겠"

## 결과 (2026-11-10)
- PR #26(F1 도메인·V10) · #27(F2 생성) · #28(F3 API) · #29(F4 화면)
- 새 면접에서 꼬리질문 없음/한 번/두 번(인성 제외)을 고르면, 답을 올린 뒤 AI가 그 답에 실제로 있는 말을 근거로 한 번 더 파고든다. 실패·지연은 꼬리 없이 넘어간다.
- 리뷰에서 바뀐 것: 잠그기 전에 답변을 읽지 않기(영속성 컨텍스트의 옛 값), 판단 중 표시를 모든 예외에서 지우기, AI 호출 스레드 상한, 근거 최소 6자, `InterviewProperties`
- 남은 것: 실제 키로 8초 목표 재기, `LlmClient` 요청별 타임아웃, 꼬리 사슬 일관성(consistency) 기준
