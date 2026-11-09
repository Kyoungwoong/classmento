import clsx from "clsx";
import type { ReactNode } from "react";
import { TwoColumn } from "./TwoColumn";

interface ScreenProps {
  /** 본문 위 작은 바(뒤로가기 등). 없으면 비워둔다. */
  header?: ReactNode;
  /** 주요 행동 하나. 보통 <BottomCTA> — 모바일은 하단 고정, md 이상은 본문 끝에 놓인다. */
  cta?: ReactNode;
  children: ReactNode;
  className?: string;
  /** site: 넓은 본문(1120px, 2단 가능) / narrow: 로그인·짧은 폼(560px) */
  width?: "site" | "narrow";
  /** 있으면 데스크톱 2단: 왼쪽 aside(질문·요약·단계), 오른쪽 본문 + 주요 행동. 모바일은 위아래로 쌓는다 */
  aside?: ReactNode;
  /** 스크롤해도 왼쪽 aside를 붙잡아 둔다 */
  stickyAside?: boolean;
}

/**
 * 페이지 본문 뼈대. 사이트 헤더·푸터는 루트 레이아웃이 감싼다.
 * 규칙: 주요 행동은 cta 슬롯 하나 — docs/design/design-system.md
 */
export function Screen({
  header,
  cta,
  children,
  className,
  width = "site",
  aside,
  stickyAside = false,
}: ScreenProps) {
  return (
    <main
      className={clsx(
        "mx-auto w-full px-5 md:px-8",
        width === "narrow" ? "max-w-narrow" : "max-w-site",
      )}
    >
      {header && <div className="flex h-14 items-center">{header}</div>}
      <div
        className={clsx(
          header ? "pt-4 md:pt-6" : "pt-8 md:pt-14",
          cta ? "pb-32 md:pb-20" : "pb-16 md:pb-20",
          className,
        )}
      >
        {aside ? (
          <TwoColumn aside={aside} stickyAside={stickyAside}>
            {children}
            {cta}
          </TwoColumn>
        ) : (
          <>
            {children}
            {cta}
          </>
        )}
      </div>
    </main>
  );
}

interface ScreenTitleProps {
  title: ReactNode;
  description?: ReactNode;
  /** 제목 위 작은 라벨(예: "2단계 · 조건") */
  eyebrow?: ReactNode;
}

/** 화면 상단의 큰 제목 + 한두 줄 설명. 제목은 사용자에게 말을 거는 구어체 한 문장. */
export function ScreenTitle({ title, description, eyebrow }: ScreenTitleProps) {
  return (
    <div className="mb-8 md:mb-10">
      {eyebrow && <p className="mb-2 text-caption font-semibold text-accent">{eyebrow}</p>}
      <h1 className="text-title1 whitespace-pre-line text-text-strong md:text-title1-lg">
        {title}
      </h1>
      {description && <p className="mt-2 text-body2 text-text-sub md:text-body1">{description}</p>}
    </div>
  );
}
