import clsx from "clsx";

/** 골랐음을 알리는 체크(색만으로 상태를 전하지 않게). 안 골랐으면 자리만 차지하고 보이지 않는다. */
export function CheckMark({ shown }: { shown: boolean }) {
  return (
    <span aria-hidden className={clsx("shrink-0", shown ? "text-accent" : "text-transparent")}>
      <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
        <path
          d="M4 10.5l4 4 8-9"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </span>
  );
}
