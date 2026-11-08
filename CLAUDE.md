# classmento

말로 답하는 모의면접 서비스. 면접 유형 4가지: ① 기술(직무·연차·스택·CS, 질문 은행) ② 이력서 기반 기술(이력서 RAG) ③ 꼬리질문(답변을 듣고 실시간으로 파고듦) ④ 인성(공통 질문). 음성 답변 → STT → AI 평가. 유형 정의: [docs/adr/0004-interview-modes.md](docs/adr/0004-interview-modes.md)

## 지도
| 경로 | 내용 | 규칙 |
|---|---|---|
| `frontend/` | Next.js 16 + Tailwind v4 (pnpm) | [frontend/CLAUDE.md](frontend/CLAUDE.md) |
| `backend/` | Spring Boot 4.1 + Java 17 (Gradle) | [backend/CLAUDE.md](backend/CLAUDE.md) |
| `docs/` | 제품·설계·규칙 문서 (아래 목록) | 코드와 함께 갱신 |
| `scripts/verify.sh` | 전체 검증 (FE lint·type·test·build + BE spotless·test) | 작업 완료 전 필수 |
| `.claude/` | skills, agents, hooks, 권한 | — |

## 먼저 읽을 문서 (작업 종류별)
- 무엇을 만드나 / 용어: [docs/product/overview.md](docs/product/overview.md)
- 구조 / 외부 연동(Claude, CLOVA): [docs/architecture.md](docs/architecture.md)
- 화면 작업: [docs/design/design-system.md](docs/design/design-system.md), [docs/design/screens.md](docs/design/screens.md)
- API 추가·변경: [docs/api/README.md](docs/api/README.md)
- 프롬프트·평가: [docs/ai/interview-and-rubric.md](docs/ai/interview-and-rubric.md)
- 코드 규칙: [docs/conventions/frontend.md](docs/conventions/frontend.md), [docs/conventions/backend.md](docs/conventions/backend.md), [DDD·TDD](docs/conventions/ddd-tdd.md), [Git·PR](docs/conventions/git.md)
- 왜 이 스택인가: [docs/adr/](docs/adr/)

## 명령
```bash
bash scripts/db.sh up                  # PostgreSQL 17 + pgvector (:5433, WSL Docker). 백엔드 실행·테스트 전 필수
bash scripts/verify.sh                 # 전체 검증 (FE만: --fe, BE만: --be). DB 자동 기동
cd backend && ./gradlew bootRun        # API :8080, Swagger /swagger-ui.html (키 없으면 가짜 AI 모드)
cd frontend && pnpm dev                # 웹 :3000
cd frontend && pnpm gen:api            # 백엔드 실행 중일 때 OpenAPI → TS 타입
```

## 불변 규칙
1. **계약은 OpenAPI가 원본.** 백엔드 API를 바꾸면 같은 작업에서 `pnpm gen:api` → 프론트 컴파일 통과까지.
2. **프롬프트는 `backend/src/main/resources/prompts/*.vN.md`에만.** 코드에 프롬프트 문자열 금지. 의미 변경은 새 버전.
3. **AI 호출은 백엔드에서만**, 포트 인터페이스(`LlmClient`/`EmbeddingClient`/`SttClient`)를 통해서만. 자동 테스트에서 실제 API 호출 금지(가짜 사용).
3-1. **스키마 변경은 새 Flyway 파일**(`V{n}__*.sql`)로만. 적용된 마이그레이션 수정 금지. 모든 사용자 데이터 테이블은 `user_id`를 갖고, 조회는 본인 것만(남의 것은 404).
4. **UI는 디자인 시스템 토큰·컴포넌트만.** 금지 목록(그라데이션, 이모지 아이콘, 카드 남발, 임의 색/px) 위반 금지.
5. **비밀값 커밋 금지.** 키는 환경변수(`.env.example` 참고). `.env*`는 읽지도 쓰지도 않는다.
6. **완료 = `bash scripts/verify.sh` 통과.** 화면 변경이면 스크린샷 확인까지(`ui-screen` 스킬).
7. 여러 단계 기능은 `docs/exec-plans/active/`에 계획부터(`feature` 스킬).
7-1. **백엔드는 DDD + TDD.** 컨텍스트 × 계층 구조와 의존 방향은 ArchUnit으로 강제한다. 테스트를 먼저 쓰고, 커밋은 red/green 단위로 잘게 나눈다 → [docs/conventions/ddd-tdd.md](docs/conventions/ddd-tdd.md)
8. **main에 직접 커밋·푸시 금지.** 모든 변경은 브랜치 → PR → 리뷰 → 머지(아래 작업 절차).

## 작업 절차 (매 요청) — 상세: [docs/conventions/git.md](docs/conventions/git.md), `ship` 스킬
```
계획(사용자와 합의) → main pull → 브랜치 생성 → 기능마다 잘게 커밋 → verify → push + PR(템플릿)
→ pr-reviewer 서브에이전트 리뷰(PR 코멘트, VERDICT) → [필수] 반영 커밋 → 재리뷰 (최대 3회)
→ ci-ok 통과 + APPROVE → merge commit으로 머지 → main checkout + pull → study-log
```
- 역할: 마스터·개발 = 메인 세션, 리뷰 = `pr-reviewer`(개발 맥락을 넘기지 않고 PR 번호만 전달)
- 커밋은 Conventional Commits(`feat(scope): 요약`). 리뷰 반영은 새 커밋으로 하고 force push 금지.
- 3회 리뷰 후에도 REQUEST_CHANGES거나, 계획에 없던 큰 결정이 필요하면 멈추고 사용자에게 묻는다.
- CI/CD: [docs/ops/deploy.md](docs/ops/deploy.md) (CI = `.github/workflows/ci.yml`, CD는 골격만)

## 작업 기록 (study-log) — 매 요청마다 필수
사용자가 나중에 복습할 수 있도록, **파일을 바꾸거나 명령을 실행한 요청은 끝내기 전에 반드시** 기록을 남긴다.
`study-log/`는 git에 올리지 않는 개인 학습용 폴더다(.gitignore). 질문에 답만 한 요청은 생략 가능.

- 위치: `study-log/YYYY-MM-DD/NN-<영문-slug>.md` (NN은 그날의 순번 01, 02 …)
- 템플릿: `study-log/_template.md`. 같은 날 같은 주제의 후속 요청이면 기존 파일 아래에 "## 추가 작업 (HH:MM)"으로 이어 쓴다.
- 날짜별 목차 `study-log/README.md`에 한 줄 추가: `- YYYY-MM-DD [제목](경로) — 한 줄 요약`
- 내용 (각 항목 짧고 구체적으로, 초보자도 이해할 수 있게):
  1. **요청** — 사용자가 원한 것 한두 문장
  2. **한 일** — 만든/바꾼 파일과 이유 (파일 경로 링크)
  3. **업무 흐름** — 이 작업이 전체 흐름(이력서 → 질문 → 녹음 → STT → 평가)이나 개발 흐름(계획 → BE → 계약 → FE → verify)의 어디에 해당하고, 데이터·요청이 어떻게 흘러가는지. 필요하면 화살표 다이어그램
  4. **새로 들어온 기술** — 처음 쓰인 라이브러리·도구·개념이 있으면: 무엇인지 / 왜 골랐는지 / 이 프로젝트에 어떻게 적용했는지(설정 파일·코드 위치) / 다음에 직접 쓸 때 순서. 없으면 "없음"
  5. **결정과 대안** — 고민했던 선택지와 고른 이유
  6. **확인 방법** — 직접 돌려볼 명령어
  7. **다음에 할 일 / 공부거리**

## 스킬
`feature`(기능 구현 흐름) · `ship`(브랜치→커밋→PR→리뷰→머지) · `ui-screen`(화면 제작+리뷰) · `api-endpoint`(API 추가+타입 생성) · `interview-ai`(프롬프트·루브릭) · `verify`(검증)

## 서브에이전트
`pr-reviewer`(PR 독립 리뷰) · `design-reviewer`(화면 디자인 규칙) · `contract-checker`(FE/BE API 계약)

## 응답 언어
사용자와는 한국어로 대화한다. 코드 식별자는 영어, 주석·문서·UI 문구는 한국어.
