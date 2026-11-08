interface ProgressBarProps {
  /** 0~1 */
  value: number;
  label?: string;
}

/** 면접 진행도 등 단계 표시. 얇은 막대 하나, 색은 primary만. */
export function ProgressBar({ value, label }: ProgressBarProps) {
  const pct = Math.round(Math.min(1, Math.max(0, value)) * 100);
  return (
    <div
      role="progressbar"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={pct}
      aria-label={label}
      className="h-1 w-full overflow-hidden rounded-full bg-bg-muted"
    >
      <div
        className="h-full bg-primary transition-[width] duration-300"
        style={{ width: `${pct}%` }}
      />
    </div>
  );
}
