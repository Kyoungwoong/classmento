import clsx from "clsx";

interface ToggleProps {
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}

/** 켜고 끄는 설정 한 줄. 왼쪽 라벨, 오른쪽 스위치. 줄 전체가 눌린다. */
export function Toggle({ label, checked, onChange }: ToggleProps) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      onClick={() => onChange(!checked)}
      className="flex h-11 w-full items-center justify-between text-left text-body2 text-text"
    >
      <span>{label}</span>
      <span
        aria-hidden
        className={clsx(
          "flex h-6 w-11 items-center rounded-full p-0.5 transition-colors duration-150",
          checked ? "bg-primary" : "bg-gray-300",
        )}
      >
        <span
          className={clsx(
            "size-5 rounded-full bg-bg transition-transform duration-150",
            checked && "translate-x-5",
          )}
        />
      </span>
    </button>
  );
}
