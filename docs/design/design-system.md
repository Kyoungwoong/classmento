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
| `BottomCTA` + `Button` | 하단 고정 주요 행동. 버튼 높이 56(lg) |
| `ListRow` | 정보 나열. 카드 그리드 대신 세로 리스트 |
| `TextField` | 회색 면 입력창(테두리 없음), 라벨은 위 |
| `ProgressBar` | 면접 진행도 (얇은 4px 막대) |
| `Toast` | "저장했어요" 같은 짧은 결과 |

새 컴포넌트는 기존 걸로 안 될 때만 만들고, 이 표에 추가한다.

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
