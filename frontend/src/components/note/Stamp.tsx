import clsx from "clsx";

/** 채점 도장: 기울어진 원형 이중 테두리 안에 점수. 리포트 종합 점수에만 쓴다. */
export function Stamp({ score, className }: { score: number; className?: string }) {
  const value = score.toFixed(1);
  return (
    <div
      role="img"
      aria-label={`종합 ${value}점, 5점 만점`}
      className={clsx(
        "relative inline-flex size-32 shrink-0 -rotate-12 flex-col items-center justify-center rounded-full border-4 border-accent text-accent md:size-40",
        className,
      )}
    >
      <span aria-hidden="true" className="absolute inset-1.5 rounded-full border border-accent" />
      <span className="text-display leading-none tabular-nums">{value}</span>
      <span className="mt-1 text-caption font-semibold">/ 5점</span>
    </div>
  );
}
