import clsx from "clsx";

interface SelectChipProps {
  label: string;
  selected: boolean;
  onClick: () => void;
}

/** 눌러서 고르는 칩(필터·여러 개 선택). 고르면 연한 블루, 아니면 회색. 높이 44px. */
export function SelectChip({ label, selected, onClick }: SelectChipProps) {
  return (
    <button
      type="button"
      aria-pressed={selected}
      onClick={onClick}
      className={clsx(
        "inline-flex h-11 shrink-0 items-center rounded-full px-4 text-body2 transition-colors duration-100",
        selected ? "bg-primary-weak font-semibold text-primary" : "bg-bg-muted text-text",
      )}
    >
      {label}
    </button>
  );
}
