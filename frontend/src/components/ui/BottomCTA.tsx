import type { ReactNode } from "react";

interface BottomCTAProps {
  children: ReactNode;
  /** CTA 위에 붙는 짧은 안내(예: "약 15분 걸려요") */
  caption?: ReactNode;
}

/**
 * 주요 행동 영역. 버튼은 1개(최대 2개: 보조 + 주요).
 * 모바일: 화면 하단 고정(엄지가 닿는 곳). md 이상: 고정을 풀고 본문 흐름 끝에 왼쪽 정렬로 놓는다.
 */
export function BottomCTA({ children, caption }: BottomCTAProps) {
  return (
    <div
      data-bottom-cta
      className="fixed inset-x-0 bottom-0 z-20 border-t border-border bg-bg px-5 pt-3 pb-[max(--spacing(5),env(safe-area-inset-bottom))] md:static md:mt-10 md:border-0 md:bg-transparent md:p-0"
    >
      {caption && (
        <p className="mb-3 text-center text-caption text-text-sub md:text-left">{caption}</p>
      )}
      <div className="flex gap-2 *:flex-1 md:*:min-w-48 md:*:flex-none">{children}</div>
    </div>
  );
}
