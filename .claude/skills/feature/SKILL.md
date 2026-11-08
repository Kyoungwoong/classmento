---
name: feature
description: classmento에 새 기능(이력서 업로드, 면접 세션, 녹음, 평가, 리포트 등)을 처음부터 끝까지 만들 때 사용. 백엔드 API·프론트 화면·AI 프롬프트 중 둘 이상을 건드리는 작업이면 이 스킬로 시작한다.
---

# 기능 개발 흐름

## 1. 맥락 읽기 (생략 금지)
- `docs/product/overview.md` — 용어·흐름·MVP 범위 밖 항목 확인. 범위 밖이면 사용자에게 먼저 묻는다.
- `docs/architecture.md` — 어느 패키지에 들어가는지.
- 화면이 있으면 `docs/design/screens.md`의 해당 화면.

## 2. 계획 파일
`docs/exec-plans/README.md` 템플릿으로 `docs/exec-plans/active/<오늘날짜>-<slug>.md` 작성.
API 모양, 화면 경로, 사용할 프롬프트 버전, 단계 체크리스트를 채운다. 애매한 결정은 사용자에게 질문.

## 3. 구현 순서
1. **백엔드** — `api-endpoint` 스킬 절차. 서비스 단위 테스트 + 컨트롤러 MockMvc 테스트. AI는 Fake로.
2. **AI** — 프롬프트가 필요하면 `interview-ai` 스킬 절차.
3. **계약** — 백엔드 실행 후 `cd frontend && pnpm gen:api`.
4. **프론트** — `ui-screen` 스킬 절차.

각 단계가 끝날 때마다 계획 파일 체크박스를 갱신하고, 바뀐 결정은 "결정 로그"에 적는다.

## 4. 마무리
- `bash scripts/verify.sh` 통과.
- `docs/api/README.md` 엔드포인트 표, `docs/design/screens.md`(화면 변경 시)를 실제와 맞춘다.
- 계획 파일을 `docs/exec-plans/completed/`로 옮기고 "결과"를 쓴다.

## 브랜치·커밋·PR
구현을 시작하기 전에 `ship` 스킬 1단계로 브랜치를 만든다. 위 3번의 각 단계가 끝날 때마다 잘게 커밋하고, 마무리 후 `ship` 4단계(PR)부터 이어간다.
