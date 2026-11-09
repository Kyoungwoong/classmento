import type { ReactNode } from "react";

interface CheckItem {
  title: ReactNode;
  description: ReactNode;
  /** 끝난 항목은 손으로 그은 체크 + 스크린리더 "완료" */
  done?: boolean;
}

/** 노트에 적은 할 일 목록: 네모 칸 + 손 체크, 행 사이 점선. 준비 단계 안내에 쓴다. */
export function CheckList({ items, label }: { items: CheckItem[]; label: string }) {
  return (
    <ol aria-label={label} className="flex flex-col">
      {items.map((it, i) => (
        <li key={i} className="flex gap-4 border-b border-dashed border-rule py-5 last:border-b-0">
          <span
            aria-hidden="true"
            className="relative mt-0.5 size-7 shrink-0 rounded-sm border-2 border-ink bg-bg"
          >
            {it.done && (
              <svg viewBox="0 0 32 28" className="absolute -top-2 -left-0.5 size-9 overflow-visible">
                <path
                  d="M4 15 L12 23 L29 2"
                  fill="none"
                  strokeWidth="3.5"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  className="stroke-accent"
                />
              </svg>
            )}
          </span>
          <span className="min-w-0">
            <span className="block text-body1 font-semibold text-text-strong">
              {it.title}
              {it.done && <span className="sr-only"> 완료</span>}
            </span>
            <span className="mt-1 block text-body2 text-text-sub">{it.description}</span>
          </span>
        </li>
      ))}
    </ol>
  );
}
