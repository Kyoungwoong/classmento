import { ProgressBar } from "./ProgressBar";

interface ScoreBarProps {
  label: string;
  /** 1~5 (평균이면 소수) */
  score: number;
  max?: number;
  /** 평균처럼 소수 자리를 고정할 때(정수여도 3.0) */
  decimals?: number;
}

/** 기준 하나의 점수: 왼쪽 이름, 오른쪽 숫자, 아래 얇은 막대(ProgressBar). 색은 primary 하나. */
export function ScoreBar({ label, score, max = 5, decimals }: ScoreBarProps) {
  const shown =
    decimals !== undefined
      ? score.toFixed(decimals)
      : Number.isInteger(score)
        ? String(score)
        : score.toFixed(1);
  return (
    <div>
      <div className="mb-2 flex items-baseline justify-between">
        <span className="text-body2 text-text">{label}</span>
        <span className="text-body2 font-semibold text-text-strong tabular-nums">{shown}</span>
      </div>
      <ProgressBar value={score / max} label={`${label} ${shown}점`} />
    </div>
  );
}
