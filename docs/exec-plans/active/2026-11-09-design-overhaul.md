# 디자인 전면 개편: 연두 메인, 웹사이트형 레이아웃, 마스코트

진행: [x] D1 기반 · [x] D2 홈·로그인 · [ ] D3 면접 화면 · [ ] D4 이력서·관리자

## Context
- 사용자가 지금 디자인이 마음에 들지 않는다고 했다(TODO.md).
  - 밋밋하다.
  - 웹앱처럼 보인다: 모바일 폭 480px 한 줄이 데스크톱에서도 그대로다.
  - 색이 너무 없다: 회색에 블루 하나뿐이다.
- 원인은 디자인 시스템의 원칙 7("모바일 폭 단일 컬럼")과 지나친 금지 목록(카드·장식 금지)이다.
- 방향: 웹사이트형 구성
  - 상단 헤더와 메뉴, 넓은 본문(약 1120px)
  - 섹션마다 배경색 띠(흰색, 연한 색, 회색)
  - 왼쪽 제목 · 오른쪽 내용의 2단
  - 테두리가 있는 패널
  - 번호 단계 목록
  - 히어로의 과정 일러스트와 표정 있는 캐릭터
- 사용자와 정한 것
  - **색**: 밝은 연두 `#A3E635`가 메인이고 버튼 글자는 남색 `#14213D`다. 남색은 제목·헤더·푸터에 쓴다. 연한 면은 `#F4FCE3`, 포인트는 노랑 `#FACC15`(조금만).
  - **작업 화면**: 데스크톱에서 넓게 2단으로 놓는다(예: 질문 | 녹음 패널, 종합 | 문항). 모바일에서는 위아래로 쌓는다.
  - **그림**: 아이콘(lucide)과 과정 일러스트, **마스코트 캐릭터**까지 넣는다.

## 디자인 원칙 (새 design-system.md)
1. **웹사이트처럼**
   - 모든 화면에 `SiteHeader`(로고, 메뉴, 로그인 상태)와 `SiteFooter`(남색 띠)를 둔다.
   - 본문 폭은 `max-w-site`(1120px)다. 폼·로그인은 `narrow`(560px)로 가운데 둔다.
2. **색은 연두 · 남색 · 연한 면 3가지가 기본, 노랑은 포인트**
   - 연두는 면과 버튼에만 쓴다. 흰 바탕 위 글자로 쓰면 읽히지 않으므로, 글자 강조는 진한 연두 `accent`(#4D7C0F)로 한다.
3. **섹션은 배경 띠로 나눈다**: `Section tone="plain|tint|muted|ink"`
4. **묶음은 패널로 한다**
   - `Panel`: 흰 면, 1px `border`, `rounded-xl`
   - 패널 안에 패널을 넣지 않는다. 그림자는 띄운 요소(토스트·시트)에만 쓴다.
5. **데스크톱은 2단, 모바일은 1단**: `TwoColumn`(왼쪽 aside는 sticky 가능)
6. **주요 행동은 하나**
   - 모바일에서는 하단에 고정한다(`BottomCTA`).
   - md 이상에서는 고정을 풀고 내용 흐름 안(패널 아래)에 둔다.
7. **숫자는 크게, 캐릭터는 상태를 말할 때만**: 마스코트는 히어로, 빈 상태, 로딩, 녹음 중, 결과에만 쓴다. 장식으로 반복하지 않는다.
- 유지할 것: 문구 톤("~해요"), 터치 44px, 상태 4종, 토큰만 쓰기(check-design.sh), 그라데이션·글래스·이모지·보라 금지
- 풀 것: "카드 금지"는 패널을 허용하는 것으로 바꾼다. "히어로 금지"는 마케팅식 3열 카드만 금지하는 것으로 바꾼다.

## 토큰 (`frontend/src/styles/tokens.css` + `globals.css @theme`)
- **lime**: 50 `#F7FEE7`, 100 `#ECFCCB`, 200 `#D9F99D`, 400 `#A3E635`(메인), 500 `#84CC16`(pressed), 700 `#4D7C0F`(글자 강조)
- **navy**: 900 `#14213D`, 800 `#22325A`, 600 `#4A5A7E`, 100 `#E6EAF2`
- **yellow**: 400 `#FACC15`, 100 `#FEF9C3`
- **gray**: 그대로 둔다. 상태 색(green·orange·red)도 그대로 둔다.
- **semantic**

  | 토큰 | 값 | 쓰임 |
  |---|---|---|
  | `primary` | lime-400 | 버튼·면 |
  | `primary-pressed` | lime-500 | 누를 때 |
  | `on-primary` | navy-900 | 연두 위 글자 |
  | `primary-weak` | `#F4FCE3` | 연한 면 |
  | `accent` | lime-700 | 링크·체크 같은 글자 강조 |
  | `ink` | navy-900 | 제목·헤더 |
  | `highlight` | yellow-400 | 포인트 |
  | `text-strong` | navy-900 | 진한 글자 |
- **블루 토큰을 지운다.** 그래서 지금 `text-primary`, `bg-blue-100`을 쓰는 곳은 모두 고쳐야 한다(타입체크로는 안 잡히니 grep으로 찾는다).
  - `text-primary` → `text-accent`
  - `bg-primary text-white` → `bg-primary text-on-primary`
- 레이아웃: `--container-site: 1120px`, `--container-narrow: 560px`, `rounded-xl` 20px
- 타입: `display` 48/40(모바일)을 히어로와 점수에 쓰고, `title1`은 데스크톱에서 32로 키운다(md 이상 반응형 클래스).

## 컴포넌트 (`frontend/src/components/ui`, 새로 만들거나 바꿀 것)
- **새로 만들기**
  - `SiteHeader`: 로고 "classmento", 메뉴(면접 보기·내 이력서·최근 리포트), 로그인/로그아웃. 모바일은 햄버거 대신 오른쪽 핵심 링크 하나 + 메뉴 시트.
  - `SiteFooter`
  - `Section`
  - `Panel`
  - `TwoColumn`
  - `StepList`: 번호 단계 행
  - `Mascot`
    - 인라인 SVG. 둥근 연두 몸통, 남색 눈, 작은 마이크를 든 "면접 친구"다.
    - 표정(mood): `idle` / `listening` / `thinking` / `happy` / `oops`
    - `size`를 받는다. 장식이면 `aria-hidden`이다.
  - `ProcessIllustration`: 이력서 → 질문 → 녹음 → 리포트 4단계 SVG. 히어로에 쓴다.
- **바꾸기**
  - `Screen`: `max-w-app` 고정을 없애고 `width="site|narrow"`를 받는다. 헤더 슬롯(TopBar)은 그대로 둔다.
  - `BottomCTA`: md 이상에서 static으로 바꾼다.
  - `Button`: secondary는 navy-100 면에 navy 글자다.
  - `Toast`
- 아이콘은 `lucide-react`(새 의존성)를 쓰고, 버튼·목록 행·섹션 제목에만 넣는다.
- 레이아웃: 루트 `app/layout.tsx`에서 `SiteHeader`와 `SiteFooter`로 모든 페이지를 감싼다. 로그인 상태는 클라이언트에서 `api.me`로 확인한다.

## 화면 적용
| 화면 | 바뀌는 모습 |
|---|---|
| 홈 `/` | 히어로 2단(왼쪽 큰 문구 + 연두 CTA, 오른쪽 `ProcessIllustration` 패널 + 마스코트) → 연한 면 띠에 "면접 흐름" `StepList` → "4가지 면접" 2단(왼쪽 제목 · 오른쪽 탭 목록) → 로그인했으면 히어로 아래에 "최근 면접" 패널 |
| 로그인·회원가입 | muted 띠 위 가운데 narrow 패널, 마스코트 idle |
| 이력서 올리기·상세 | 2단: 왼쪽 안내(무엇을 뽑는지, 마스코트), 오른쪽 업로드 패널. 상세는 왼쪽 요약·기술, 오른쪽 경험 목록 |
| 새 면접 | 2단: 왼쪽 단계 진행(1 종류 · 2 조건 · 3 질문 수, 현재 단계 연두), 오른쪽 선택 패널. 모바일은 지금처럼 위 진행 막대 |
| 면접 진행 | 2단: 왼쪽 진행도 + 질문(크게) + 질문 종류 배지, 오른쪽 녹음 패널(타이머 display, 마스코트 listening/thinking, 버튼). 모바일은 쌓고 CTA는 하단 고정 |
| 리포트 | 상단 연한 면 띠: 종합 점수 display + 마스코트 happy + 잘한 점/보완할 점 → 2단: 왼쪽 기준별 ScoreBar 패널(sticky), 오른쪽 문항 목록 패널 |
| 문항 상세 | 2단: 왼쪽 질문 · 녹음 · 내 답변, 오른쪽 점수 · 근거 · 피드백 패널 |
| 관리자 | site 폭 + 패널. 구조는 그대로 두고 색만 따라 바뀐다 |
- 빈 상태, 로딩, 에러에는 마스코트 표정을 함께 넣는다(`thinking`, `oops`).

## PR 나누기 (매번 작업 루프: 브랜치 → 잘게 커밋 → verify → PR → pr-reviewer → 머지)
| PR | 브랜치 | 내용 |
|---|---|---|
| D1 | `feat/design-foundation` | ADR 0006(디자인 방향 전환), design-system.md 새로 쓰기, 토큰·@theme, `text-primary`/블루 사용처 교체, Button·Screen·BottomCTA·Toast, Section·Panel·TwoColumn·StepList, lucide-react, SiteHeader/Footer를 레이아웃에, design-reviewer 에이전트와 ui-screen 스킬 기준 갱신 |
| D2 | `feat/design-home-auth` | Mascot·ProcessIllustration, 홈 랜딩, 로그인·회원가입 → **사용자에게 스크린샷으로 방향 확인** |
| D3 | `feat/design-interview` | 새 면접, 진행, 리포트, 문항 상세 2단 + 마스코트 상태 |
| D4 | `feat/design-resume-admin` | 이력서 올리기·상세, 관리자 화면 정리, screens.md 전체 갱신 |
- 단위 테스트는 역할·문구로 찾으므로 레이아웃이 바뀌어도 대부분 그대로 돈다. 새 컴포넌트(SiteHeader 로그인 상태, Mascot의 aria, BottomCTA)에는 테스트를 추가한다.

## Verification
- PR마다 `bash scripts/verify.sh --fe` 통과(check-design: 임의 hex·px, 그라데이션 금지는 유지)
- 375 / 768 / 1280 세 폭에서 스크린샷을 찍고, design-reviewer(새 기준)가 "위반 없음"을 낸다.
- 대비 확인: 연두 버튼의 남색 글자, 흰 바탕의 accent 글자가 WCAG AA(4.5:1)를 넘는지 계산해 문서에 적는다.
- 브라우저에서 처음부터 끝까지 확인한다(가짜 AI): 홈 → 로그인 → 새 면접 → 녹음 3개 → 리포트 → 문항 상세.
- D2가 끝나면 사용자에게 홈과 로그인 스크린샷을 보여 주고 방향을 확인한 뒤 D3로 넘어간다.
- 끝나면 로컬 TODO.md 체크, study-log, 서버 정리.
