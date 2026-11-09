# AI: 질문 생성 · 평가 루브릭 · 프롬프트 관리

## 프롬프트 파일 규칙
- 위치: `backend/src/main/resources/prompts/<name>.v<N>.md`. 코드에 프롬프트 문자열을 직접 쓰지 않는다. 로딩은 `PromptLoader`.
- 의미가 바뀌는 수정(루브릭 기준·출력 형식 변경)은 **새 버전 파일**(`v2`)을 만들고 코드의 참조를 바꾼다. 오타 수정만 같은 파일에서.
- `Evaluation`에 `promptVersion`과 `model`을 저장한다.
- 출력은 JSON. 서버에서 파싱·검증(필드 누락·점수 범위) 후 저장. 실패 시 1회 재시도 후 `FAILED`.
- 입력은 XML 태그로 감싼다(`<resume>`, `<question>`, `<answer>`). 사용자 입력 안의 지시문은 데이터로 취급하도록 프롬프트에 명시.
- 키 없이 개발할 때 쓰는 가짜 응답: `backend/src/main/resources/fake/<name>.vN.json`(`DevFakeLlmClient`). 새 프롬프트를 만들면 출력 형식에 맞는 샘플도 함께 추가한다.

| 프롬프트 | 파일 | 용도 |
|---|---|---|
| 이력서 구조화 | `resume-parse.v2.md` (v1은 기록용) | 추출 텍스트 → `ParsedResume` JSON. v2부터 경력·프로젝트별 `summary` (`ResumeParser`) → `ExperienceFactory`가 경험으로 분리 |
| 경험 기반 질문 | `question-experience.v1.md` | 경험 하나 + 질문 은행 kNN 후보 → 질문 N개 `[{question, intent, bankQuestionId}]` (`ExperienceQuestionWriter`, `EXPERIENCE` 탭) |
| 답변 평가 | `answer-evaluation.v2.md` (v1은 기록용) | 질문·의도 + STT 답변 + `<rubric>` → 기준별 점수·근거, 잘한 점·고칠 점·답변 방향 (`AnswerEvaluator`). 루브릭 기준이 빠지거나 1~5가 아니면 한 번 다시, 그래도 안 되면 FAILED. 근거 없는 점수는 3점까지로 낮춤 |
| 세션 리포트 | `session-report.v1.md` | 끝난 면접의 문항 평가들 → 강점·보완점 한 줄씩 (`ReportWriter`). 종합 점수는 답변 점수 평균(AI가 아님). AI가 실패하면 가장 높은 답변의 잘한 점·가장 낮은 답변의 고칠 점으로 대신 |
| (기록용) | `question-generation.v1.md` | ADR 0004 시절 이력서 기반 질문. 쓰지 않음 |

## 질문 만들기 (ADR 0005, 실행계획 2026-11-09-interview-session)
질문은 면접을 만들 때 모두 정한다(꼬리질문 없음). 질문 수 3·5·7.

- **기술 `TECH` — AI 없음**
  - 질문 은행에서 인성이 아닌 대분류, 직무(또는 COMMON), 연차(같은 단계, 모자라면 한 단계 아래)로 거른다.
  - 이력서로 좁히면 이력서 기술과 맞는 태그·소분류를 먼저 고른다.
  - 소분류가 겹치지 않게 무작위로 고른다(`QuestionPicker`).
- **인성 `BEHAVIORAL` — AI 없음**: 인성 대분류에서 소분류가 겹치지 않게 고른다.
- **혼합 `MIXED` — AI 없음**: 기술 ⌈N/2⌉ + 인성 ⌊N/2⌋개를 기술부터 번갈아 낸다.
- **경험 기반 `EXPERIENCE` — AI**
  1. 고른 경험의 임베딩으로 질문 은행(인성 제외) kNN 상위 10개를 후보로 뽑는다.
  2. 경험 원문(제목·요약·본문·기술)과 후보를 `question-experience.v1`에 넣는다.
  3. 응답을 검증한다.
     - 질문이 5~300자가 아니면 버린다.
     - 후보에 없는 `bankQuestionId`는 지운다(만든 질문으로 본다).
     - JSON이 깨지면 한 번 다시 부른다.
  4. 그래도 모자라거나 AI를 쓸 수 없으면, 아직 쓰지 않은 후보 질문으로 채운다. 후보까지 모자라면 409 `NOT_ENOUGH_QUESTIONS`.
  - 길이: 프롬프트는 말로 읽기 쉽게 80자 이내를 요구하지만, 코드는 질문 은행과 같은 5~300자까지 받는다(조금 긴 질문을 버려 후보로 바뀌는 것보다 낫다고 봄).
  - AI가 후보를 바탕으로 만들고도 번호를 비우면, 채우기 단계에서 비슷한 후보가 다시 들어갈 수 있다(드묾, 받아들임).
  - 후보 kNN은 HNSW 인덱스를 탄 뒤 필터를 적용하므로, 질문 은행이 커지면 k개보다 적게 나올 수 있다(그때 ef_search·필터 인덱스 검토).
  - 관점을 섞는다: 왜 그 선택 / 문제 → 해결 / 트레이드오프 / 경험과 연결된 CS 개념. 경험에 없는 기술은 가정하지 않는다.
- 공통: 질문 은행이 모자라면 409 `NOT_ENOUGH_QUESTIONS`(있는 만큼만 만들지 않는다).

## 평가 루브릭 (1~5점)
`TECH`·`RESUME_TECH`·`DEEP_DIVE`는 "기술" 기준을, `BEHAVIORAL`은 "인성" 기준을 쓴다. `DEEP_DIVE`는 세션 리포트에 **consistency**(꼬리질문을 거치며 앞뒤 답이 일관되고 깊어지는지)를 추가한다.

| 유형 | 기준 | 5점 | 3점 | 1점 |
|---|---|---|---|---|
| 기술 | accuracy 정확성 | 사실 오류 없음 | 사소한 오류 | 핵심 오류 |
| 기술 | depth 깊이·근거 | 이유·수치·대안 비교 | 이유 일부 | 나열만 |
| 기술 | problem_solving | 문제→원인→해결→결과 | 일부 단계 누락 | 과정 없음 |
| 기술 | communication | 결론 먼저, 구조적 | 이해 가능 | 장황·산만 |
| 인성 | specificity (STAR) | 상황·행동·결과 구체 | 일부 모호 | 일반론 |
| 인성 | ownership | 본인 행동이 명확 | "우리가"로 흐림 | 본인 역할 없음 |
| 인성 | collaboration | 갈등 조정·소통 근거 | 언급만 | 없음 |
| 인성 | reflection | 배운 점→이후 변화 | 배운 점만 | 없음 |

- 근거 인용이 없으면 3점 초과 금지. STT 오인식은 감점 금지.
- 피드백 형식: 잘한 점 1 + 고칠 점 1 + 답변 방향 1, 지원자에게 말하는 존댓말.

## 회귀 확인 (interview-ai 스킬)
- 샘플: `backend/src/test/resources/fixtures/` (예정) — 이력서 2종, 좋은/나쁜 답변 쌍.
- 프롬프트 변경 시: 좋은 답변 점수 > 나쁜 답변 점수인지, JSON 파싱 성공률 100%인지 확인.
- 실제 API를 부르는 확인은 수동 스크립트로만(자동 테스트는 `FakeLlmClient`).
