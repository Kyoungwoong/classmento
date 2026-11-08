import clsx from "clsx";
import type { ReactNode } from "react";

interface ScreenProps {
  /** 상단 내비게이션 영역(뒤로가기 등). 없으면 비워둔다. */
  header?: ReactNode;
  /** 화면 하단에 고정되는 단 하나의 주요 행동. 보통 <BottomCTA>. */
  cta?: ReactNode;
  children: ReactNode;
  className?: string;
}

/**
 * 모든 페이지의 기본 레이아웃. 모바일 폭(최대 480px) 단일 컬럼, 데스크톱에서도 같은 컬럼을 가운데 둔다.
 * 규칙: 한 화면에 하나의 일, 주요 행동은 cta 슬롯 하나 — docs/design/design-system.md
 */
export function Screen({ header, cta, children, className }: ScreenProps) {
  return (
    <div className="mx-auto flex min-h-dvh w-full max-w-app flex-col bg-bg">
      {header && <header className="flex h-14 items-center px-5">{header}</header>}
      <main className={clsx("flex-1 px-5 pt-6", cta ? "pb-32" : "pb-10", className)}>
        {children}
      </main>
      {cta}
    </div>
  );
}

interface ScreenTitleProps {
  title: ReactNode;
  description?: ReactNode;
}

/** 화면 상단의 큰 제목 + 한두 줄 설명. 제목은 사용자에게 말을 거는 구어체 한 문장. */
export function ScreenTitle({ title, description }: ScreenTitleProps) {
  return (
    <div className="mb-8">
      <h1 className="text-title1 whitespace-pre-line text-text-strong">{title}</h1>
      {description && <p className="mt-2 text-body2 text-text-sub">{description}</p>}
    </div>
  );
}
