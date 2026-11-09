import clsx from "clsx";
import { Check } from "lucide-react";

export interface ProgressStep {
  key: string;
  label: string;
  hint: string;
}

/**
 * 세로 단계 목록: 끝난 단계는 연두 원 + 체크, 지금 단계는 연한 면 행, 남은 단계는 남색 연한 원.
 * 새 면접 만들기의 데스크톱 왼쪽 열에 쓴다(모바일은 진행 막대).
 */
export function StepProgress({
  steps,
  current,
  label,
}: {
  steps: ProgressStep[];
  current: number;
  label: string;
}) {
  return (
    <ol aria-label={label} className="flex flex-col gap-1">
      {steps.map((s, i) => {
        const done = i < current;
        const now = i === current;
        return (
          <li
            key={s.key}
            aria-current={now ? "step" : undefined}
            className={clsx("flex items-center gap-4 rounded-lg px-4 py-3", now && "bg-bg-tint")}
          >
            <span
              className={clsx(
                "flex size-9 shrink-0 items-center justify-center rounded-full text-body2 font-semibold tabular-nums",
                done || now ? "bg-primary text-on-primary" : "bg-navy-100 text-ink",
              )}
            >
              {done ? <Check size={18} aria-hidden /> : i + 1}
            </span>
            <span className="min-w-0">
              <span className="block text-body1 font-semibold text-text-strong">{s.label}</span>
              <span className={clsx("block text-caption", now ? "text-text" : "text-text-sub")}>
                {done ? `${s.hint} · 완료` : s.hint}
              </span>
            </span>
          </li>
        );
      })}
    </ol>
  );
}
