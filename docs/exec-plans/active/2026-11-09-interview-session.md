# 실행계획: 면접 세션 (질문 고르기 → 녹음 → STT → 평가 → 리포트)

진행: [x] E1 · [ ] E2 · [ ] E3 · [ ] E4 · [ ] E5 · [ ] E6

## Context
- 질문 은행(PR #9~#13)과 이력서 경험 분리가 끝났으니, 이제 서비스 본체인 **면접 세션**을 만든다.
- 탭은 ADR 0005의 4개(경험 기반 / 기술 / 인성 / 혼합)를 모두 만든다.
- 사용자와 합의한 것:
  - **4개 탭 모두**
    - 기술·인성·혼합: 질문 은행에서 고른다(AI 없음).
    - 경험 기반: 경험 + 질문 은행 kNN 후보를 바탕으로 AI가 질문을 만든다.
  - **평가 시점**
    - 경험 기반·기술·인성: 답변마다 바로 평가한다(백그라운드에서 STT → 평가).
    - 혼합: 면접이 끝난 뒤 한꺼번에 평가한다(STT는 답변마다 미리 해 둔다).
  - **녹음은 보관한다.** 리포트에서 다시 듣고, STT 텍스트도 함께 보여준다(본인만).
  - **꼬리질문은 이번에 없다.** 질문은 세션을 만들 때 모두 만들어 둔다.
- 크레딧(결제)은 이번 범위가 아니다.

## 도메인 — 새 컨텍스트 `com.classmento.interview`
| 애그리거트 | 내용 | 규칙 (TDD) |
|---|---|---|
| `InterviewSession` | `userId`, `type`(EXPERIENCE·TECH·BEHAVIORAL·MIXED), 조건(`level`, `jobRole`, `resumeId?`, `experienceId?`), `status`(IN_PROGRESS → FINISHED → REPORTED), 질문 목록 | 질문 수는 3·5·7. 경험 기반이면 이력서와 경험이 필수. 끝난 세션에는 답할 수 없다. 끝내기는 모든 질문에 답한 뒤에만(건너뛰기 없음). |
| `InterviewQuestion` (세션 안 엔티티) | `seq`, `text`, `intent`, `kind`(TECH·BEHAVIORAL → 루브릭 선택), `bankQuestionId?`, `generated` | 같은 세션에서 같은 질문 은행 질문은 한 번만 |
| `Answer` | `questionSeq`, `audioPath`, `durationSec`, `transcript`, `status`(UPLOADED → TRANSCRIBED → EVALUATED / FAILED), `Evaluation`(기준별 점수·근거, 잘한 점·고칠 점·답변 방향, `promptVersion`, `model`) | 다시 녹음하면 덮어쓴다(끝나기 전까지만). 점수는 1~5. 근거가 없으면 3점 초과 금지(파싱할 때 검증). |
| `SessionReport` | 기준별 평균, 종합 점수, 강점 한 줄과 보완점 한 줄(`session-report.v1`) | 평가가 모두 끝나야 만든다 |

**다른 컨텍스트 접근**
- interview는 resume·questionbank의 엔티티를 직접 쓰지 않는다.
  - interview 도메인에 포트 `ExperienceSource`, `QuestionSource`를 둔다.
  - 구현은 `interview.infrastructure` 어댑터가 맡고, 각 컨텍스트의 **application 조회 서비스**만 부른다.
- 새로 만드는 조회 서비스:
  - resume `ExperienceQuery`: 내 경험 목록, 경험 하나와 그 임베딩(본인 것만, 남의 것은 404)
  - questionbank `QuestionCatalog`: 조건으로 무작위 고르기(소분류가 겹치지 않게), 벡터로 kNN(기존 `QuestionEmbeddingRepository.findNearest` 재사용)
- 인성 대분류는 이름 상수 `"인성"`으로 구분한다(문서화).
- `ArchitectureTest`
  - `CONTEXTS`에 `interview`를 추가한다.
  - 규칙 추가: interview는 `interview.infrastructure`에서만, 그리고 resume·questionbank의 `application`만 의존한다.
  - 규칙 추가: resume·questionbank는 interview에 의존하지 않는다.

## 질문 만들기
- **TECH**
  - 범위: 인성이 아닌 대분류, 직무(`jobRole` 또는 COMMON), 난이도(같은 단계, 모자라면 한 단계 아래까지).
  - 이력서를 고르면 이력서 기술과 맞는 태그·소분류를 먼저 고른다.
  - 고를 때 소분류가 겹치지 않게 한다.
- **BEHAVIORAL**: 인성 대분류에서 고르고, 소분류가 겹치지 않게 한다.
- **MIXED**: 기술 ⌈N/2⌉ + 인성 ⌊N/2⌋개를 번갈아 낸다.
- **EXPERIENCE**
  1. 경험 임베딩으로 질문 은행 kNN 상위 10개를 뽑는다(인성 제외).
  2. 경험 원문(제목·요약·기술·하이라이트)과 후보를 `question-experience.v1.md`에 넣는다.
  3. JSON `[{question, intent, bankQuestionId|null}]` N개를 받는다.
  - 규칙: 경험에 적힌 것만 다룬다. "왜 그 선택 / 문제 → 해결 / 트레이드오프 / 관련 CS 개념"을 섞는다. 80자 이내.
  - 파싱·검증에 실패하면 한 번 재시도하고, 그래도 실패하면 kNN 후보 상위 N개를 그대로 쓴다.
  - 가짜 응답: `fake/question-experience.v1.json`
- 질문 은행에 질문이 모자라면 있는 만큼만 만들지 않고 409 `NOT_ENOUGH_QUESTIONS`("조건에 맞는 질문이 부족해요. 조건을 넓혀 주세요.")를 낸다.

## 답변 → STT → 평가
1. `POST /api/interviews/{id}/questions/{seq}/answer` (multipart 오디오, webm/ogg/m4a/wav, 최대 10MB·3분)
   - 기존 `FileStorage`에 저장 → 상태 UPLOADED → 바로 202.
2. 백그라운드 실행기(기존 `ResumeExecutorConfig` 패턴, 테스트에서는 동기)
   - `SttClient.transcribe` → TRANSCRIBED
   - 혼합이 아니면 이어서 평가: `answer-evaluation.v2.md`
     - 입력: `<question>`, `<intent>`, `<answer>`, `<rubric>`(TECH 또는 BEHAVIORAL)
     - JSON을 검증하고, 실패하면 한 번 재시도한 뒤 FAILED
     - EVALUATED
   - 외부 호출은 트랜잭션 밖에서, 결과 저장은 트랜잭션 안에서 한다(기존 패턴).
3. `POST /api/interviews/{id}/finish`
   - FINISHED로 바꾼다. 혼합이면 이때 전체를 평가한다(비동기).
   - 모든 평가가 끝나면 `session-report.v1.md`로 종합 → REPORTED.
4. FAILED 답변은 `POST …/answer/{seq}/retry`로 STT·평가만 다시 한다.
- **키 없이 개발**: `DevFakeSttClient`를 추가한다(`classmento.ai.fake=true`일 때 고정 문장). 가짜 LLM JSON도 함께 추가한다.

## API (`/api/interviews/**`, 로그인 사용자, 본인 것만 · 남의 것은 404)
```
POST /api/interviews                          {type, count, level?, jobRole?, resumeId?, experienceId?, narrowByResume?} → 201 세션(질문 포함)
GET  /api/interviews                          내 면접 목록(최신순)
GET  /api/interviews/{id}                     세션 + 질문 + 답변 상태
POST /api/interviews/{id}/questions/{seq}/answer   multipart audio → 202 {status}
GET  /api/interviews/{id}/questions/{seq}/audio    녹음 스트리밍(본인)
POST /api/interviews/{id}/questions/{seq}/retry    실패한 STT·평가 다시
POST /api/interviews/{id}/finish              → 202
GET  /api/interviews/{id}/report              {status: EVALUATING|READY, overall, criteria[], strength, improvement, items[{seq, question, transcript, scores, feedback, answerStatus}]}
```
- 스키마: **V7** `interview_session`, `interview_question`, `interview_answer`(모두 `user_id`, `ON DELETE CASCADE`)
- 평가 점수는 `jsonb`로 저장한다.

## 화면 (토스풍, `ui-screen` + design-reviewer, 375/1280)
- `/interview/new` (숨고식, 한 화면에 한 질문)
  1. "어떤 면접을 볼까요?"
     - 선택형 ListRow 4개. 경험 기반은 경험이 있는 이력서가 없으면 설명에 "이력서가 필요해요" + 비활성.
  2. 탭별 조건
     - 경험 기반: (이력서가 여럿이면 이력서 고르기) → 경험 배너 고르기(기존 `ExperienceBanner` 재사용)
     - 기술·혼합: 직무 SelectChip → 연차 SegmentedControl. 기술은 이력서가 있으면 "내 이력서 기술로 좁히기" Toggle.
     - 인성: 바로 질문 수로
  3. 질문 수 3 / 5 / 7 → [질문 만들기]
     - 로딩 문구: "질문 은행에서 고르고 있어요" / 경험 기반은 "경험을 읽고 질문을 만들고 있어요"
- `/interview/[id]` 진행 화면 (한 페이지 안에서 단계 전환)
  - ProgressBar + "2 / 5", 질문(`text-title1`)
  - 녹음 상태: 대기 / 녹음 중(경과 시간 큰 숫자) / 완료(다시 듣기)
  - CTA: [답변 시작하기] → [답변 끝내기] → [다시 녹음](보조) + [다음 질문]. 마지막은 [면접 끝내기].
  - 업로드 실패 시 그 자리에서 [다시 올리기]
  - 마이크 권한 거부 안내
  - `useRecorder` 훅(MediaRecorder): 3분이 되면 자동으로 멈춘다.
- `/interview/[id]/report`
  - "면접이 끝났어요" + 종합 점수 큰 숫자, 강점과 보완점 한 줄씩
  - 문항 ListRow(질문 + 점수, 평가 중이면 "평가하고 있어요", 실패면 [다시 평가하기])
  - 평가가 끝날 때까지 3초마다 다시 불러온다.
- `/interview/[id]/report/[seq]`
  - 녹음 다시 듣기(`<audio>`)와 내 답변(STT 텍스트)
  - 기준별 점수 막대(ProgressBar 재사용)
  - 잘한 점 / 고칠 점 / 이렇게 답해 보세요
  - CTA [다시 연습하기]
- 진입점
  - 홈과 이력서 화면의 [면접 준비하기]를 활성화한다.
  - 홈에 "최근 면접" 목록을 둔다(`GET /api/interviews`).
- 새 공통 컴포넌트: `ScoreBar`(기준 이름 + 점수 막대). 나머지는 기존 컴포넌트를 쓴다.

## PR 나누기 (각 800줄 이내 목표, 매번 작업 루프)
| PR | 브랜치 | 내용 |
|---|---|---|
| E1 | `feat/interview-session` | ArchitectureTest → 세션·질문 도메인 red/green → V7 → `QuestionCatalog`·`ExperienceQuery` 조회 서비스 → 기술·인성·혼합 질문 고르기 → 세션 만들기·조회 API(목록 포함) |
| E2 | `feat/interview-experience-questions` | `question-experience.v1` 프롬프트 + 가짜 응답 → 경험 kNN 후보 → 생성·검증·대체 → EXPERIENCE 세션 |
| E3 | `feat/interview-answers` | Answer·Evaluation 도메인 → 업로드·오디오 스트리밍 → 비동기 STT·평가(`answer-evaluation.v2`) → 끝내기·혼합 일괄 평가·`session-report.v1` → 리포트 API, `DevFakeSttClient` |
| E4 | `feat/interview-new-ui` | `/interview/new` 단계 폼 + 홈·이력서 진입점 |
| E5 | `feat/interview-progress-ui` | `useRecorder` + 진행 화면 |
| E6 | `feat/interview-report-ui` | `ScoreBar` + 리포트·문항 상세 + 홈 "최근 면접" |

**문서**
- `docs/exec-plans/active/2026-11-09-interview-session.md`
- `docs/api/README.md`, `docs/architecture.md`(interview 행, 컨텍스트 의존 규칙)
- `docs/ai/interview-and-rubric.md`: ADR 0005 기준으로 질문 전략을 다시 쓰고, 프롬프트 표를 갱신
- `docs/design/screens.md`(4·5·6절을 ADR 0005로 갱신)
- study-log

## Verification
- PR마다 `bash scripts/verify.sh` 통과, red → green 커밋, pr-reviewer APPROVE, `ci-ok`
- 도메인 단위 테스트
  - 질문 수
  - 경험 기반 필수 조건
  - 끝난 세션에는 답할 수 없음
  - 다시 녹음하면 덮어쓰기
  - 점수 범위·근거 규칙
  - 혼합은 끝낸 뒤 평가
- 통합 테스트(가짜 LLM·STT, 동기 실행기)
  - 탭별 세션 만들기, 질문이 모자라면 409, 남의 세션은 404
  - 업로드 → TRANSCRIBED → EVALUATED
  - 혼합은 끝내기 전까지 평가하지 않음
  - 리포트 READY, 오디오는 본인만
- 경험 기반: 가짜 응답이 잘못된 JSON이면 재시도 후 kNN으로 대체하는지
- 브라우저(로컬, 가짜 AI)
  - 4개 탭으로 면접을 만든다.
  - 실제 마이크로 녹음 → 다음 질문 → 끝내기
  - 리포트에서 점수·STT 텍스트·다시 듣기 확인
  - 375/1280 스크린샷, design-reviewer "위반 없음"
- 끝나면 서버·DB keep-alive·Gradle·Playwright를 정리한다.
