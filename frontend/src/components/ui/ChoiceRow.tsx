import clsx from "clsx";
import { useId, type ReactNode } from "react";

interface ChoiceRowProps {
  title: ReactNode;
  description?: ReactNode;
  selected: boolean;
  disabled?: boolean;
  onSelect: () => void;
}

/**
 * 하나만 고르는 목록의 행(숨고식 "어떤 면접을 볼까요?"). 부모에 role="radiogroup"과 `-mx-4`를 둔다
 * (행 안쪽 px-4와 합쳐 글자가 화면 기준선 px-5에 맞고, 고른 면만 바깥으로 넓어진다).
 * 고르면 연한 블루 면 + 오른쪽 체크. 고를 수 없으면 제목만 흐리게, 이유(description)는 읽히게 둔다.
 */
export function ChoiceRow({ title, description, selected, disabled, onSelect }: ChoiceRowProps) {
  const descriptionId = useId();
  return (
    <button
      type="button"
      role="radio"
      aria-checked={selected}
      disabled={disabled}
      aria-describedby={description ? descriptionId : undefined}
      onClick={onSelect}
      className={clsx(
        "flex min-h-11 w-full items-center gap-4 rounded-md px-4 py-4 text-left transition-colors duration-100",
        selected ? "bg-primary-weak" : "bg-bg enabled:active:bg-bg-subtle",
      )}
    >
      <span className="min-w-0 flex-1">
        <span
          className={clsx(
            "block text-body1 font-medium",
            disabled ? "text-text-hint" : "text-text-strong",
          )}
        >
          {title}
        </span>
        {description && (
          <span id={descriptionId} className="mt-0.5 block text-body2 text-text-sub">
            {description}
          </span>
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
