import clsx from "clsx";
import type { ReactNode } from "react";

interface StickyNoteProps {
  title: ReactNode;
  children: ReactNode;
  /** 제목 단계(기본 h2) */
  level?: "h2" | "h3";
  className?: string;
}

/** 노랑 포스트잇 + 테이프. 화면마다 팁 하나에만 쓴다(종이 위에 붙인 것이라 그림자 허용). */
export function StickyNote({ title, children, level = "h2", className }: StickyNoteProps) {
  const Heading = level;
  return (
    <div
      className={clsx(
        "relative rotate-1 rounded-sm bg-highlight-weak px-5 pt-8 pb-5 shadow-float md:px-6",
        className,
      )}
    >
      <span
        aria-hidden="true"
        className="absolute -top-3 left-1/2 h-6 w-20 -translate-x-1/2 -rotate-3 bg-lime-200"
      />
      <Heading className="font-hand text-title2 text-ink">{title}</Heading>
      <div className="mt-2 text-body1 text-text">{children}</div>
    </div>
  );
}
