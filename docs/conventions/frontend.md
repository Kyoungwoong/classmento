# 프론트엔드 규칙

스택: Next.js 16(App Router, `src/`), React 19, TypeScript, Tailwind v4, Vitest + Testing Library, pnpm.
**Next.js 16은 학습 데이터와 다르다.** 코드 작성 전에 `frontend/node_modules/next/dist/docs/`의 해당 가이드를 읽는다(`frontend/AGENTS.md`).

## 구조
```
src/
  app/                  # 라우트. page.tsx는 조립만, 로직은 features로
  components/ui/        # 디자인 시스템 컴포넌트 (도메인 모름)
  features/<domain>/    # resume, interview, report — 화면 조각·훅·상태
  lib/api/              # client.ts(유일한 fetch 진입점), schema.d.ts(생성물)
  styles/tokens.css     # 디자인 토큰
```

## 규칙
- 스타일: 토큰 클래스만. 임의값(`text-[#333]`, `p-[13px]`), 인라인 style 색상 금지. 예외는 동적 수치(진행률 width)뿐.
- 컴포넌트: 서버 컴포넌트 기본. 상태·브라우저 API(녹음 등)가 필요한 곳만 `"use client"`, 가능한 한 잎(leaf)에.
- API: `lib/api/client.ts`의 함수만 사용. 응답 타입은 `schema.d.ts`에서. 컴포넌트에서 `fetch` 직접 호출 금지.
- 녹음: `features/interview/useRecorder.ts`(예정)에 MediaRecorder 캡슐화. 권한 거부·미지원 브라우저 상태를 반드시 처리.
- 문구: [design-system.md#문구-톤](../design/design-system.md). 하드코딩 한국어 OK(MVP는 i18n 없음).
- 테스트: 컴포넌트 동작은 `*.test.tsx`로 같은 폴더에. 스냅샷 테스트 금지.
- 포맷: Prettier(+tailwind 클래스 정렬). 훅이 편집 시 자동 실행.

## 명령
`pnpm dev` · `pnpm lint` · `pnpm typecheck` · `pnpm test` · `pnpm build` · `pnpm gen:api` · `pnpm format`
