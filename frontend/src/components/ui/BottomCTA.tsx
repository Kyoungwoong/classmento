import type { ReactNode } from "react";

interface BottomCTAProps {
  children: ReactNode;
  /** CTA 위에 붙는 짧은 안내(예: "약 15분 걸려요") */
  caption?: ReactNode;
}

/** 화면 하단 고정 행동 영역. 버튼은 1개(최대 2개: 보조 + 주요)만 둔다. */
export function BottomCTA({ children, caption }: BottomCTAProps) {
  return (
    <div className="fixed inset-x-0 bottom-0 mx-auto w-full max-w-app bg-bg px-5 pt-3 pb-[max(--spacing(5),env(safe-area-inset-bottom))]">
      {caption && <p className="mb-3 text-center text-caption text-text-sub">{caption}</p>}
      <div className="flex gap-2 [&>*]:flex-1">{children}</div>
    </div>
  );
}
