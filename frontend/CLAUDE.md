@AGENTS.md

# frontend

규칙 전문: [../docs/conventions/frontend.md](../docs/conventions/frontend.md) · 디자인: [../docs/design/design-system.md](../docs/design/design-system.md)

## 꼭 지킬 것
- Next.js 16 API는 추측하지 말고 `node_modules/next/dist/docs/`에서 확인(위 AGENTS.md).
- 페이지는 `<Screen>` + `<ScreenTitle>` + `<BottomCTA>`로 시작한다. `src/components/ui`를 먼저 재사용.
- 색·글자·간격은 토큰 클래스만(`text-title1`, `text-text-sub`, `bg-primary`, `max-w-app` …). `[#hex]`, `[13px]` 금지 — verify가 잡는다.
- Tailwind 기본 팔레트는 꺼져 있다. `bg-blue-500`이 아니라 `bg-primary`.
- 백엔드 호출은 `src/lib/api/client.ts`만. 타입은 `schema.d.ts`(생성물, 수정 금지).
- 브라우저 API(MediaRecorder 등)를 쓰는 컴포넌트만 `"use client"`.

## 명령
`pnpm dev` · `pnpm lint` · `pnpm typecheck` · `pnpm test` · `pnpm build` · `pnpm gen:api` · `pnpm format`
