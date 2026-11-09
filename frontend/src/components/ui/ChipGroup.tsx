import { useId, type ReactNode, type Ref } from "react";

interface ChipGroupProps {
  label: string;
  /** 라벨 옆 작은 안내(예: "여러 개 고를 수 있어요") */
  hint?: string;
  error?: string;
  /** SelectChip들. 없을 때 안내 문구를 넣어도 된다. */
  children: ReactNode;
  /** 검증 실패 시 이 묶음으로 스크롤할 때 쓴다 (React 19: 일반 prop) */
  ref?: Ref<HTMLDivElement>;
}

/**
 * SelectChip 묶음. 라벨·에러를 group에 이어서 스크린리더가 "무엇을 고르는지"와 "무엇이 틀렸는지"를 읽게 한다.
 */
export function ChipGroup({ label, hint, error, children, ref }: ChipGroupProps) {
  const labelId = useId();
  const errorId = useId();
  return (
    <div ref={ref}>
      <p className="mb-2 text-caption text-text-sub">
        <span id={labelId}>{label}</span>
        {hint && <span className="text-text-hint">{` · ${hint}`}</span>}
      </p>
      <div
        role="group"
        aria-labelledby={labelId}
        aria-describedby={error ? errorId : undefined}
        className="flex flex-wrap gap-2"
      >
        {children}
      </div>
      {error && (
        <p id={errorId} className="mt-2 text-caption text-danger">
          {error}
        </p>
      )}
    </div>
  );
}
