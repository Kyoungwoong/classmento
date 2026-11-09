import clsx from "clsx";
import type { ReactNode } from "react";

interface ChoiceRowProps {
  title: ReactNode;
  description?: ReactNode;
  selected: boolean;
  disabled?: boolean;
  onSelect: () => void;
}

/**
 * 하나만 고르는 목록의 행(숨고식 "어떤 면접을 볼까요?"). 부모에 role="radiogroup"을 둔다.
 * 고르면 연한 블루 면 + 오른쪽 체크. 높이는 ListRow처럼 넉넉하게.
 */
export function ChoiceRow({ title, description, selected, disabled, onSelect }: ChoiceRowProps) {
  return (
    <button
      type="button"
      role="radio"
      aria-checked={selected}
      disabled={disabled}
      onClick={onSelect}
      className={clsx(
        "flex w-full items-center gap-4 rounded-md px-4 py-4 text-left transition-colors duration-100",
        selected ? "bg-primary-weak" : "bg-bg active:bg-bg-subtle",
        disabled && "opacity-50",
      )}
    >
      <span className="min-w-0 flex-1">
        <span className="block text-body1 font-medium text-text-strong">{title}</span>
        {description && (
          <span className="mt-0.5 block text-body2 text-text-sub">{description}</span>
        )}
      </span>
      <span
        aria-hidden
        className={clsx("shrink-0", selected ? "text-primary" : "text-transparent")}
      >
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
    </button>
  );
}
