# 디자인 시스템

목표: 토스·숨고·직방처럼 **설명 없이도 다음에 뭘 할지 아는 화면**. "AI가 만든 랜딩페이지" 느낌을 없앤다.
토큰 원본: `frontend/src/styles/tokens.css`, Tailwind 연결: `frontend/src/app/globals.css`.

## 7가지 원칙
1. **한 화면에 한 가지 일.** 업로드 화면은 업로드만, 질문 화면은 질문과 녹음만.
2. **주요 행동은 하단 고정 버튼 하나.** `<Screen cta={<BottomCTA>...}>`. 보조 행동이 필요하면 왼쪽에 `secondary` 하나까지.
3. **제목은 말 걸듯이.** "이력서를 올려주세요", "준비되면 답변을 시작해요". 명사형 제목("이력서 업로드") 금지.
4. **구분은 여백과 회색 단계로.** 테두리·그림자·카드 박스를 기본으로 쓰지 않는다. 묶음이 필요하면 `bg-bg-muted` 면 하나.
5. **색은 회색 + 블루 하나.** 강조는 `primary` 한 색. 상태(성공/경고/위험)는 결과 표시에만.
6. **숫자와 결과는 크게.** 점수·시간·진행도는 큰 숫자 + 짧은 라벨.
7. **모바일 폭 단일 컬럼.** 최대 480px, 데스크톱에서도 가운데 같은 컬럼. 좌우 여백 20px(`px-5`).

## 토큰
| 분류 | 값 | Tailwind |
|---|---|---|
| 글꼴 | Pretendard Variable | `font-sans`(기본) |
| 타입 | 26 bold / 22 bold / 17 / 15 / 13 | `text-title1` `text-title2` `text-body1` `text-body2` `text-caption` |
| 텍스트 색 | gray-900 / 800 / 600 / 500 | `text-text-strong` `text-text` `text-text-sub` `text-text-hint` |
| 배경 | white / gray-50 / gray-100 | `bg-bg` `bg-bg-subtle` `bg-bg-muted` |
| 강조 | #3182f6 (pressed #2272eb, weak #e8f3ff) | `bg-primary` `text-primary` `bg-primary-weak` |
| 상태 | green / orange / red | `text-success` `text-warning` `text-danger` |
| 모서리 | 8 / 12 / 16 | `rounded-sm` `rounded-md` `rounded-lg` (버튼·입력 12) |
| 간격 | 4의 배수 | `p-1`=4px … 화면 좌우 `px-5`, 섹션 간 `mt-8`~`mt-10` |
| 그림자 | 1단계(토스트·시트 전용) | `shadow-float` |

Tailwind 기본 팔레트는 꺼져 있다(`--color-*: initial`). `bg-purple-500`, `shadow-xl` 같은 클래스는 생성되지 않는다.

## 컴포넌트 (`frontend/src/components/ui`)
| 컴포넌트 | 언제 |
|---|---|
| `Screen`, `ScreenTitle` | 모든 페이지의 뼈대와 큰 제목 |
| `BottomCTA` + `Button` | 하단 고정 주요 행동. 버튼 높이 56(lg). 로딩 문구는 `loadingText`로 지금 하는 일("올리고 있어요") |
| `ButtonLink` | 화면 이동용 버튼(모양은 Button, 실제는 링크) |
| `TopBar` | `Screen` header 슬롯. 뒤로 가기 + 오른쪽 작은 텍스트 행동 하나(예: 로그아웃) |
| `Chip` | 기술 같은 짧은 키워드. `size` md(편집, 지우기 버튼 터치 44px) / sm(읽기 전용), `tone` muted(흰 배경 위) / surface(회색 면 위) |
| `ListRow` | 정보 나열. 카드 그리드 대신 세로 리스트. 긴 문장(질문)은 `titleLines={2}` |
| `CheckMark` | 고른 상태 표시(ChoiceRow·고르는 ExperienceBanner). 색만으로 상태를 전하지 않게 `text-primary` 체크. 장식용으로 쓰지 않는다 |
| `ChoiceRow` | 하나만 고르는 목록 행(면접 종류 등). 부모 `role="radiogroup"` + `-mx-4`, 고르면 `bg-primary-weak` + 체크, 고를 수 없으면 제목만 흐리고 이유는 `text-text-sub` |
| `SelectChip` | 눌러서 고르는 칩(필터·여러 개 선택). 높이 44, 고르면 `bg-primary-weak` + `text-primary` |
| `ChipGroup` | SelectChip 묶음 + 라벨·에러(스크린리더용 group 연결). 줄바꿈으로 나열 |
| `SegmentedControl` | 2~5개 중 하나 고르기(난이도). 회색 면 위 고른 칸만 흰 면, `radiogroup` |
| `Toggle` | 켜고 끄는 설정 한 줄(왼쪽 라벨, 오른쪽 스위치, `switch`) |
| `TextArea` | 여러 줄 입력. TextField와 같은 회색 면, `maxLength`면 글자 수 |
| `TagInput` | Notion식 태그: 입력하면 후보, Enter·쉼표로 만들기, 칩 지우기, 한글 조합 중 Enter 무시 |
| `TextField` | 회색 면 입력창(테두리 없음), 라벨은 위 |
| `ProgressBar` | 면접 진행도 (얇은 4px 막대) |
| `Toast` | "저장했어요" 같은 짧은 결과 |

새 컴포넌트는 기존 걸로 안 될 때만 만들고, 이 표에 추가한다.

## 터치 · 상태
- 누르는 것은 모두 높이 44px 이상(`h-11`). 작은 텍스트 버튼·링크도 `inline-flex h-11 items-center`로 영역을 넓힌다.
- 화면마다 4가지 상태를 그린다: 처음 불러오기("~를 불러오고 있어요") / 처리 중 / 비어 있음 / 에러(원인 + 다음 행동 CTA).
- 에러는 위치를 맞춘다: 특정 칸 문제는 그 칸 아래, 그 외는 폼 아래 `role="alert"`.
- 칩 여러 개는 가로 스크롤 대신 줄바꿈(`ChipGroup`, flex-wrap). 가로로 숨기면 데스크톱 마우스로 끝까지 가기 어렵다.
- 블루(`text-primary`)는 누를 수 있는 것과 주요 강조에만 쓴다. 상태 표시(예: "관리자")는 회색 글자로.

## 문구 톤
- "~해요" 체. 명령형("업로드하십시오") 금지, 과한 감탄("놀라운!") 금지.
- 버튼은 동사로 끝: "이력서 올리기", "답변 시작하기", "다음 질문".
- 에러는 원인 + 할 일: "파일이 너무 커요. 20MB 이하로 올려주세요."
- 로딩은 지금 하는 일: "이력서를 읽고 있어요", "답변을 듣고 있어요".

## 금지 목록 (AI스러운 디자인)
`design-reviewer` 에이전트가 이 목록으로 검사한다.
- [ ] 그라데이션 배경·그라데이션 텍스트·블러된 컬러 원(blob)
- [ ] 이모지를 아이콘처럼 쓰기 (🚀✨🎯 등)
- [ ] glassmorphism(반투명+backdrop-blur 카드)
- [ ] 히어로 + 3열 기능 카드 + "Get Started" 마케팅 레이아웃
- [ ] "혁신적인", "차세대", "AI 기반의 놀라운", "Unlock", "Elevate" 같은 문구
- [ ] 보라·핑크 계열, 네온 색, 여러 강조색 혼용
- [ ] 모든 요소를 카드 박스+테두리+그림자로 감싸기
- [ ] 의미 없는 장식 아이콘, 배지 남발
- [ ] 한 화면에 Primary 버튼 2개 이상
- [ ] 임의 hex 색(`text-[#...]`)·임의 px(`p-[13px]`)·인라인 style 색상
- [ ] 중앙 정렬 긴 본문 (본문은 왼쪽 정렬, 중앙 정렬은 캡션·빈 상태만)

## 참고 패턴
- 토스: 큰 제목 + 하단 CTA, 리스트 행, 숫자 강조, 바텀시트.
- 숨고: 단계형 질문 폼(한 화면 한 질문 + 진행 막대) → 면접 진행 화면에 적용.
- 직방: 필터·정보 밀도 있는 리스트를 회색 구분선 없이 여백으로 정리 → 결과 리포트에 적용.
