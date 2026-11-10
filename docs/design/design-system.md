# 디자인 시스템

목표: 처음 온 사람도 무엇을 하는 서비스이고 다음에 뭘 누르면 되는지 바로 아는 **밝고 산뜻한 웹사이트**. 콘셉트는 **면접 노트**다. 질문을 적고, 말로 답하고, 채점받는 노트 한 권처럼 보이게 한다. 방향 결정: [ADR 0006](../adr/0006-design-direction.md)(색·웹사이트형), [ADR 0007](../adr/0007-interview-note.md)(노트 콘셉트)
토큰 원본: `frontend/src/styles/tokens.css`, Tailwind 연결: `frontend/src/app/globals.css`.

## 8가지 원칙
1. **웹사이트처럼.**
   - 모든 화면 위에 `SiteHeader`(로고·메뉴·로그인), 아래에 남색 `SiteFooter`를 둔다(루트 레이아웃).
   - 본문 폭: `Screen width="site"`(1120px). 로그인·짧은 폼만 `width="narrow"`(560px)로 가운데 둔다.
2. **색은 연두 · 남색 · 연한 면, 노랑은 포인트.**
   - 연두(`primary`)는 버튼·막대·선택 면에 쓴다. 흰 바탕 위 **글자로 쓰지 않는다**(대비 1.5:1). 글자 강조는 `text-accent`(진한 연두)로 한다.
   - 남색(`ink`)은 제목, 헤더 버튼, 푸터 면에 쓴다.
   - 노랑(`highlight`)은 형광펜·포스트잇·배지에만, 한 화면에 세 군데까지 쓴다(원칙 4).
3. **화면마다 노트의 다른 장면.** 같은 배치(띠 → 2단 → 패널)를 모든 화면에 반복하지 않는다.
   - 섹션의 주인공 묶음은 `NoteSheet`(노트 한 장, 섹션마다 한 장까지)로, 화면 맨 위 영역은 `GridPaper`(모눈)로 깔 수 있다.
   - 홈: 모눈 표지 → 준비 체크리스트 → 면접 종류 색인 탭 → 칠판 / 로그인: 모눈 위 노트 한 장 / 리포트: 채점된 답안지(도장·형광펜·여백 메모)
4. **손맛은 섹션마다 두 개까지.** 손맛 = 형광펜(`Highlight`, 제목 핵심어 하나) · 손글씨(`HandNote`·포스트잇 제목·칠판 제목, 짧은 한마디) · 손 밑줄(`Scribble`) · 포스트잇(`StickyNote`, 팁 하나) · 도장(`Stamp`). 한 섹션에 두 개까지, 같은 부품은 화면에 한 번씩. 기울기는 그 부품과 홈 미리보기 노트에만 주고 본문·버튼·폼은 단정하게 둔다.
   - **어떤 폭에서도 가로로 넘치지 않는다.** 장식을 음수 위치로 밖에 붙이면 좁은 폭에서 페이지가 넓어진다(헤더 선·푸터가 끊겨 보임). 배치는 그리드 칸으로 하고, 루트 레이아웃 본문은 `overflow-x-clip`으로 한 번 더 막는다. 320 · 768 · 1024 · 1280 · 1920에서 `scrollWidth`가 화면 폭과 같은지 확인한다.
   - 묶음이 더 필요하면 `Panel`(흰 면, 1px 테두리, 둥근 20px, 그림자 없음). 패널·노트 안에 또 넣지 않고, 한 화면에 2~3개까지.
   - 섹션을 띠 색으로 나눌 때는 `Section tone`(흰색 · 연한 연두 · 아주 연한 회색 · 남색), 같은 톤을 연달아 두지 않는다.
5. **데스크톱 2단, 모바일 1단(필요한 화면만).**
   - `TwoColumn`: 왼쪽은 제목·요약·안내, 오른쪽은 실제로 하는 일·목록
   - 요약 점수처럼 계속 보여야 하면 `stickyAside`
6. **주요 행동은 하나.** 연두 primary 버튼은 한 화면에 하나다. 같은 행동을 다시 권할 때(홈 아래 남색 띠 등)는 secondary 모양으로 한다.
   - `BottomCTA`는 모바일에서 하단에 고정되고, md 이상에서는 본문 끝에 왼쪽 정렬로 놓인다.
   - 보조 행동은 `secondary` 하나까지
7. **숫자는 크게, 캐릭터는 상태를 말할 때만.**
   - 점수·타이머는 `text-display`. 리포트 종합 점수는 채점 도장(`Stamp`)
   - 마스코트(`Mascot`, 이름 "멘토리")는 히어로, 로그인·회원가입 인사(idle), 빈 상태, 로딩(thinking), 녹음 중(listening), 결과(happy), 문제(oops)에만 쓴다. 한 화면에 하나만 두고, 장식으로 반복하지 않는다.
8. **우리 것을 만든다.** 다른 서비스의 화면 구성을 그대로 따라 하지 않는다. 참고는 원리(큰 숫자, 단계형 폼)만 가져온다.

## 토큰
| 분류 | 값 | Tailwind |
|---|---|---|
| 글꼴 | Pretendard Variable / 손글씨 Gaegu 700 | `font-sans`(기본) / `font-hand`(`HandNote`·포스트잇 제목·칠판 제목만) |
| 타입 | hero 52 / display 40 / title1 26(md 이상 32) / title2 22 / body1 17 / body2 15 / caption 13 | `text-hero` `text-display` `text-title1 md:text-title1-lg` `text-title2` `text-body1` `text-body2` `text-caption` |
| 메인(연두) | lime-400 `#A3E635`, pressed lime-500 | `bg-primary` `active:bg-primary-pressed` |
| 연두 위 글자 | navy-900 `#14213D` | `text-on-primary` |
| 글자 강조 | lime-700 `#4D7C0F` | `text-accent` (링크·체크·단계 번호) |
| 남색 | navy-900 / 800 / 600 / 200 / 100 / 50 | `bg-ink` `text-ink` `text-on-ink` `bg-navy-100`(secondary 버튼, 눌림 `navy-200`) |
| 포인트 | yellow-400 / 100 | `bg-highlight` `bg-highlight-weak` |
| 배경 | white / 연한 연두 `#F4FCE3` / gray-50 / gray-100 | `bg-bg` `bg-bg-tint`(=`bg-primary-weak`) `bg-bg-subtle` `bg-bg-muted` |
| 글자 | navy-900 / gray-800 / 600 / 500 | `text-text-strong` `text-text` `text-text-sub` `text-text-hint` |
| 테두리 | gray-200 | `border-border` |
| 노트 | 종이 `#FFFDF6` / 모눈·줄 `#DFE5F0` / 색인 탭·테이프 lime-200 · navy-200 · navy-100 | `bg-paper` `border-rule` `stroke-rule` / `bg-lime-200` `bg-navy-200` `bg-navy-100`(탭·테이프 면에만) |
| 상태 | green / orange / red-600 `#D22030` | `text-success` `text-warning` `text-danger`(흰색 대비 5.3, danger 버튼도 AA) |
| 모서리 | 8 / 12 / 16 / 20 | `rounded-sm` `rounded-md`(버튼·입력) `rounded-lg` `rounded-xl`(패널) |
| 폭 | 1120 / 560 | `max-w-site` `max-w-narrow` |
| 간격 | 4의 배수 | 본문 좌우 `px-5 md:px-8`, 섹션 위아래 `py-14 md:py-24` |
| 그림자 | 1단계(토스트·시트·모바일 메뉴·포스트잇 전용) | `shadow-float` |

Tailwind 기본 팔레트는 꺼져 있다(`--color-*: initial`). `bg-purple-500`, `shadow-xl` 같은 클래스는 생성되지 않는다.

**대비(WCAG AA)**

| 조합 | 대비 |
|---|---|
| 남색 / 연두 | 10.6 |
| accent / 흰색 | 5.0 |
| accent / 연한 면 | 4.7 |
| 남색 / 흰색 | 16.0 |
| 연한 남색 / 남색 면 | 13.3 |
| 연두 글자 / 남색 면 | 10.6 |
| danger / 흰색 | 5.3 |
| `text-sub` / 연한 면 | 4.4 |
| accent / 남색 면 | 3.2 |

- `text-sub` / 연한 면은 기준에 모자라므로, 연한 면 위 본문은 `text-text`로 쓴다.
- accent / 남색 면도 기준에 모자란다. 남색 면 위 강조 글자는 연두(`text-primary`)를 쓴다. 연두를 글자로 쓰는 곳은 **남색 면 위뿐**이다.

## 컴포넌트
**사이트** (`frontend/src/components/site`)

| 컴포넌트 | 언제 |
|---|---|
| `SiteHeader` | 루트 레이아웃. 로고 · 메뉴(면접 보기·이력서 올리기·관리자면 관리, 지금 페이지는 `aria-current`) · 로그인/로그아웃(로그인·회원가입 화면에서는 숨김). 모바일은 메뉴 버튼과 펼침 목록(열면 첫 링크로 초점, Escape로 닫기). 페이지가 바뀌면 로그인 상태를 다시 확인한다 |
| `SiteFooter` | 루트 레이아웃. 남색 띠 |
| `Logo` | 연두 말풍선 마크 + "classmento". 홈 링크 |

**공통 UI** (`frontend/src/components/ui`)

| 컴포넌트 | 언제 |
|---|---|
| `Screen`, `ScreenTitle` | 페이지 본문 뼈대(`width`)와 큰 제목. 제목 위에 단계 라벨(`eyebrow`)을 붙일 수 있다. `aside`를 주면 데스크톱 2단(왼쪽 aside, 오른쪽 본문 + CTA), `stickyAside`로 왼쪽 고정 |
| `Section`, `SectionHeading` | 띠로 나누는 섹션. 띠 안 제목은 h2, `tone`을 띠와 같게 넘긴다. 작업 화면 아래 묶음은 `compact`(위아래 여백 작게) |
| `Panel` | 관련된 것을 한 덩어리로(녹음 패널, 점수 요약, 업로드 영역). `tone="tint"`는 강조 묶음 하나(연한 면 띠 안에는 두지 않는다) |
| `TwoColumn` | 데스크톱 2단(왼쪽 5/12 · 오른쪽 7/12, `ratio="even"`이면 반반) |
| `BottomCTA` + `Button` | 주요 행동. 버튼 높이 56(lg). primary = 연두 면 + 남색 글자, secondary = 연한 남색 면. 로딩 문구는 `loadingText`로 지금 하는 일("올리고 있어요") |
| `ButtonLink` | 화면 이동용 버튼(모양은 Button, 실제는 링크) |
| `TopBar` | `Screen` header 슬롯. 뒤로 가기 + 오른쪽 작은 텍스트 행동 하나 |
| `Chip` | 기술 같은 짧은 키워드. `size` md(편집, 지우기 버튼 터치 44px) / sm(읽기 전용), `tone` muted(흰 배경 위) / surface(회색 면 위) |
| `ListRow` | 정보 나열. 긴 문장(질문)은 `titleLines={2}` |
| `CheckMark` | 고른 상태 표시. `text-accent` 체크. 장식용으로 쓰지 않는다 |
| `ChoiceRow` | 하나만 고르는 목록 행. 부모 `role="radiogroup"` + `-mx-4`, 고르면 연한 면 + 체크 |
| `SelectChip` | 눌러서 고르는 칩. 높이 44, 고르면 연한 면 + `text-accent` |
| `ChipGroup` | SelectChip 묶음 + 라벨·에러. 줄바꿈으로 나열 |
| `SegmentedControl` | 2~5개 중 하나 고르기(난이도). 회색 면 위 고른 칸만 흰 면, `radiogroup` |
| `Toggle` | 켜고 끄는 설정 한 줄(켜짐은 연두) |
| `TextField` / `TextArea` | 회색 면 입력창(테두리 없음), 라벨은 위, 초점은 `ring-accent` |
| `TagInput` | Notion식 태그: 입력하면 후보, Enter·쉼표로 만들기, 한글 조합 중 Enter 무시 |
| `ProgressBar` | 진행도 (얇은 4px 연두 막대) |
| `ScoreBar` | 기준 하나의 점수: 이름 + 숫자 + ProgressBar. 점수로 색을 바꾸지 않는다. 평균은 `decimals={1}` |
| `Mascot` | 마스코트 멘토리(`components/site`). `mood` idle·listening·thinking·happy·oops, `label`을 주면 그림(role=img), 없으면 장식 |
| `Toast` | "저장했어요" 같은 짧은 결과(남색 면). live region은 늘 두고 내용만 바꾼다 |

**노트** (`frontend/src/components/note`, ADR 0007)

| 컴포넌트 | 언제 |
|---|---|
| `GridPaper` | 모눈 바탕(SVG 패턴, 장식). 부모에 `relative isolate`, 첫 자식으로 둔다. 화면 맨 위 영역·로그인 바탕 |
| `NoteSheet` | 노트 한 장: 종이 면 + 윗변 스프링 구멍 + 왼쪽 연두 여백선(`margin={false}`로 끔). 섹션의 주인공 묶음 하나(섹션마다 한 장까지) |
| `Highlight` | 노랑 형광펜. 제목 핵심어 하나(짧은 단어, 줄바꿈하지 않음) |
| `Scribble` | 손으로 그은 연두 밑줄(장식). 제목 핵심어 아래 한 곳 |
| `HandNote` | 손글씨 한마디(`text-accent`). 본문·버튼·폼 금지 |
| `Stamp` | 채점 도장(role=img "종합 N점, 5점 만점"). 리포트 종합 점수·홈 미리보기에만 |
| `StickyNote` | 노랑 포스트잇 + 테이프, 손글씨 제목. 팁 하나 |
| `StepProgress` | 세로 단계 목록(`features/interview`): 끝난 단계 연두 원+체크, 지금 단계 연한 연두 행(`aria-current="step"`). 새 면접 왼쪽 열 |
| `markEvidence` | 답변 글자에서 채점 근거 인용을 노랑 `<mark>`로 칠한다(두 군데까지, `features/interview`). 문항 상세 |
| `CheckList` | 네모 칸 + 손 체크 할 일 목록(준비 단계). 끝난 항목은 "완료"로 읽힌다 |

아이콘: `lucide-react`. 버튼, 목록 행, 섹션 제목 앞에만 쓰고 크기는 20~24, 장식이면 `aria-hidden`이다.
새 컴포넌트는 기존 걸로 안 될 때만 만들고, 이 표에 추가한다.

## 터치 · 상태
- 누르는 것은 모두 높이 44px 이상(`h-11`)이다. 작은 텍스트 버튼·링크도 `inline-flex h-11 items-center`로 영역을 넓힌다.
- 화면마다 4가지 상태를 그린다: 처음 불러오기("~를 불러오고 있어요") / 처리 중 / 비어 있음 / 에러(원인 + 다음 행동 CTA).
- 에러는 위치를 맞춘다: 특정 칸 문제는 그 칸 아래, 그 외는 폼 아래 `role="alert"`.
- 칩 여러 개는 가로 스크롤 대신 줄바꿈(`ChipGroup`, flex-wrap)으로 나열한다.
- 상태 표시(예: "관리자")는 회색 글자로 한다. 강조 글자(`text-accent`)는 누를 수 있는 것과 핵심 숫자·단계 번호에만 쓴다.

## 문구 톤
- "~해요" 체. 명령형("업로드하십시오") 금지, 과한 감탄("놀라운!") 금지.
- 버튼은 동사로 끝: "이력서 올리기", "답변 시작하기", "다음 질문".
- 에러는 원인 + 할 일: "파일이 너무 커요. 20MB 이하로 올려주세요."
- 로딩은 지금 하는 일: "이력서를 읽고 있어요", "답변을 듣고 있어요".

## 금지 목록 (AI스러운 디자인)
`design-reviewer` 에이전트가 이 목록으로 검사한다.
- [ ] 그라데이션 배경·그라데이션 텍스트·블러된 컬러 원(blob)
- [ ] 이모지를 아이콘처럼 쓰기 (🚀✨🎯 등). 아이콘은 lucide
- [ ] glassmorphism(반투명+backdrop-blur 카드)
- [ ] 같은 모양 3열 기능 카드 그리드 + "Get Started" 마케팅 레이아웃(히어로 자체는 허용)
- [ ] "혁신적인", "차세대", "AI 기반의 놀라운", "Unlock", "Elevate" 같은 문구
- [ ] 보라·핑크·네온 색, 팔레트 밖 강조색. 연두를 흰 바탕 글자색으로 쓰기
- [ ] 그림자 카드 남발, 패널 안 패널
- [ ] 의미 없는 장식 아이콘·배지 남발, 마스코트를 화면마다 장식으로 반복
- [ ] 한 화면에 Primary 버튼 2개 이상
- [ ] 임의 hex 색(`text-[#...]`)·임의 px(`p-[13px]`)·인라인 style 색상
- [ ] 중앙 정렬 긴 본문 (본문은 왼쪽 정렬, 중앙 정렬은 캡션·빈 상태만)

## 참고 패턴
- 토스: 큰 숫자 강조, 문구 톤
- 숨고: 단계형 질문 폼(한 단계 한 질문 + 진행) → 새 면접 화면
