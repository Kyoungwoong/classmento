---
name: ui-screen
description: classmento 프론트엔드 화면·컴포넌트를 만들거나 고칠 때 사용. 토스/숨고/직방 같은 단순하고 직관적인 UI를 유지하고 "AI스러운" 디자인(그라데이션, 이모지, 카드 남발)을 막기 위해 디자인 시스템 확인 → 구현 → 스크린샷 → design-reviewer 검토까지 진행한다.
---

# 화면 만들기

## 1. 읽기
- `docs/design/design-system.md` 전체(특히 **금지 목록**과 **문구 톤**).
- `docs/design/screens.md`의 해당 화면 와이어. 없으면 먼저 와이어를 추가하고 사용자에게 확인.
- `frontend/src/components/ui/` 목록 — 있는 걸 먼저 쓴다.
- Next.js API가 필요하면 `frontend/node_modules/next/dist/docs/`에서 확인.

## 2. 구현 규칙
- 뼈대: `<Screen cta={<BottomCTA>…</BottomCTA>}>` + `<ScreenTitle title=… description=… />`.
- 한 화면 한 가지 일, Primary 버튼 1개. 정보는 `ListRow` 세로 나열, 카드 그리드 금지.
- 토큰 클래스만 사용. 새 값이 정말 필요하면 `tokens.css` + `globals.css @theme` + 디자인 문서를 함께 고친다.
- 상태 4종을 모두 그린다: 기본 / 로딩("~하고 있어요") / 비어 있음 / 에러(원인 + 할 일).
- 화면 전용 조각은 `src/features/<domain>/`에, 범용이면 `components/ui/`에 두고 디자인 문서 표에 추가.

## 3. 눈으로 확인 (생략 금지)
1. `cd frontend && pnpm dev` (백그라운드).
2. playwright MCP로 해당 경로를 **375×812**, **1280×800** 두 크기로 스크린샷.
   (playwright MCP가 없으면 내장 브라우저 도구로 같은 작업)
3. `design-reviewer` 서브에이전트에 스크린샷 경로와 변경된 tsx 파일 목록을 넘겨 검토 요청.
4. 지적 사항을 고치고 다시 스크린샷. "위반 없음"이 나올 때까지.

## 4. 마무리
`bash scripts/verify.sh --fe` 통과 (임의 색/px·그라데이션 검사 포함).
