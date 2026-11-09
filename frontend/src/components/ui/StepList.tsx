import type { ReactNode } from "react";

interface Step {
  title: ReactNode;
  description: ReactNode;
}

/**
 * 번호 단계 목록: "01 · 제목 · 설명"을 가로 한 줄씩(모바일은 세로), 행 사이 얇은 선.
 * 연한 면(tint) 띠 안이면 `onTint` — 설명 글자를 진하게(text-sub는 연한 면 위 대비 부족).
 */
export function StepList({ steps, onTint = false }: { steps: Step[]; onTint?: boolean }) {
  return (
    <ol className="border-t border-border">
      {steps.map((s, i) => (
        <li
          key={i}
          className="grid gap-1 border-b border-border py-6 md:grid-cols-12 md:items-baseline md:gap-6 md:py-8"
        >
          <span className="text-body2 font-semibold text-accent tabular-nums md:col-span-1">
            {String(i + 1).padStart(2, "0")}
          </span>
          <span className="text-body1 font-semibold text-text-strong md:col-span-4 md:text-title2">
            {s.title}
          </span>
          <span
            className={
              onTint
                ? "text-body2 text-text md:col-span-7"
                : "text-body2 text-text-sub md:col-span-7"
            }
          >
            {s.description}
          </span>
        </li>
      ))}
    </ol>
  );
}
