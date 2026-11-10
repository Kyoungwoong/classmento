import clsx from "clsx";

/** 채점 도장: 기울어진 원형 이중 테두리 안에 점수. 점수(리포트 종합·문항, 홈 미리보기)에만 쓴다. */
export function Stamp({
  score,
  className,
  label = "종합",
  size = "lg",
}: {
  score: number;
  className?: string;
  /** 읽히는 이름 앞부분(기본 "종합", 문항 상세는 "이 문항") */
  label?: string;
  /** lg: 리포트 머리 · md: 패널 안 */
  size?: "md" | "lg";
}) {
  const value = score.toFixed(1);
  return (
    <div
      role="img"
      aria-label={`${label} ${value}점, 5점 만점`}
      className={clsx(
        "relative inline-flex shrink-0 -rotate-12 flex-col items-center justify-center rounded-full border-4 border-accent text-accent",
        size === "lg" ? "size-32 md:size-40" : "size-28",
        className,
      )}
    >
      <span aria-hidden="true" className="absolute inset-1.5 rounded-full border border-accent" />
      <span className="text-display leading-none tabular-nums">{value}</span>
      <span className="mt-1 text-caption font-semibold">/ 5점</span>
    </div>
  );
}
