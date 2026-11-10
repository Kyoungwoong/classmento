import clsx from "clsx";
import type { HTMLAttributes, ReactNode } from "react";

const HOLES = 8;

interface NoteSheetProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  /** 왼쪽 연두 여백선(기본 켬). 폭이 좁은 곳은 끈다 */
  margin?: boolean;
}

/**
 * 노트 한 장: 미색 종이 면 + 윗변 스프링 구멍 + 왼쪽 연두 여백선.
 * 섹션의 주인공 묶음 하나에 쓴다(섹션마다 한 장까지). 노트 안에 노트를 넣지 않는다.
 */
export function NoteSheet({ children, margin = true, className, ...rest }: NoteSheetProps) {
  return (
    <div
      className={clsx(
        "relative rounded-lg border border-rule bg-paper pt-12 pr-5 pb-6 md:pr-8 md:pb-8",
        margin ? "pl-10 md:pl-14" : "pl-5 md:pl-8",
        className,
      )}
      {...rest}
    >
      <div
        data-holes
        aria-hidden="true"
        className="absolute inset-x-0 top-4 flex justify-center gap-6 overflow-hidden px-3 md:gap-8"
      >
        {Array.from({ length: HOLES }, (_, i) => (
          <span key={i} className="size-3 rounded-full bg-bg-muted ring-1 ring-rule" />
        ))}
      </div>
      {margin && (
        <span aria-hidden="true" className="absolute inset-y-0 left-6 w-px bg-primary md:left-9" />
      )}
      {children}
    </div>
  );
}
