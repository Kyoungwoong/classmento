import clsx from "clsx";

interface ChipProps {
  label: string;
  /** 있으면 오른쪽에 지우기 버튼 */
  onRemove?: () => void;
  /** md: 편집용(36px) / sm: 읽기 전용 작은 표시(28px) */
  size?: "md" | "sm";
  /** muted: 흰 배경 위(회색 면) / surface: 회색 면 위(흰 면) */
  tone?: "muted" | "surface";
}

/** 기술 스택 같은 짧은 키워드. 색은 회색·흰색 면 하나만. */
export function Chip({ label, onRemove, size = "md", tone = "muted" }: ChipProps) {
  return (
    <span
      className={clsx(
        "inline-flex items-center rounded-full text-text",
        size === "md" ? "h-9 pl-3 text-body2" : "h-7 pl-3 text-caption",
        tone === "muted" ? "bg-bg-muted" : "bg-bg",
      )}
    >
      <span className={onRemove ? undefined : "pr-3"}>{label}</span>
      {onRemove && (
        // 아이콘은 작게, 누르는 영역은 44px
        <button
          type="button"
          onClick={onRemove}
          aria-label={`${label} 지우기`}
          className="-my-1 flex size-11 items-center justify-center rounded-full text-text-hint active:text-text"
        >
          <svg width="12" height="12" viewBox="0 0 12 12" aria-hidden>
            <path
              d="M3 3l6 6M9 3l-6 6"
              stroke="currentColor"
              strokeWidth="1.6"
              strokeLinecap="round"
            />
          </svg>
        </button>
      )}
    </span>
  );
}
