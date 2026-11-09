import clsx from "clsx";

export interface SegmentOption<T extends string> {
  value: T;
  label: string;
}

interface SegmentedControlProps<T extends string> {
  label: string;
  options: readonly SegmentOption<T>[];
  /** null이면 아무것도 고르지 않은 상태 */
  value: T | null;
  onChange: (value: T) => void;
}

/** 2~5개 중 하나 고르기(난이도 등). 회색 면 위에 고른 칸만 흰 면. */
export function SegmentedControl<T extends string>({
  label,
  options,
  value,
  onChange,
}: SegmentedControlProps<T>) {
  return (
    <div>
      <p className="mb-2 text-caption text-text-sub">{label}</p>
      <div role="radiogroup" aria-label={label} className="flex rounded-md bg-bg-muted p-1">
        {options.map((o) => {
          const selected = o.value === value;
          return (
            <button
              key={o.value}
              type="button"
              role="radio"
              aria-checked={selected}
              onClick={() => onChange(o.value)}
              className={clsx(
                "h-11 flex-1 rounded-sm text-body2 transition-colors duration-100",
                selected ? "bg-bg font-semibold text-text-strong" : "text-text-sub",
              )}
            >
              {o.label}
            </button>
          );
        })}
      </div>
    </div>
  );
}
