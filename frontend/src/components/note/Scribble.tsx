import clsx from "clsx";

/** 손으로 그은 연두 밑줄(장식). 제목 핵심어 아래 한 곳에만. */
export function Scribble({ className }: { className?: string }) {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 200 14"
      preserveAspectRatio="none"
      className={clsx("pointer-events-none block h-3 w-full", className)}
    >
      <path
        d="M3 9 C 40 3, 70 12, 105 7 S 165 4, 197 8"
        fill="none"
        strokeWidth="5"
        strokeLinecap="round"
        className="stroke-primary"
      />
    </svg>
  );
}
