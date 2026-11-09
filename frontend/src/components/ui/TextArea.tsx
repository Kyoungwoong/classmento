import clsx from "clsx";
import { useId, type Ref, type TextareaHTMLAttributes } from "react";

export interface TextAreaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string;
  error?: string;
  /** React 19: ref는 일반 prop으로 받아 textarea에 넘긴다 */
  ref?: Ref<HTMLTextAreaElement>;
}

/** 여러 줄 입력. TextField와 같은 회색 면, maxLength가 있으면 오른쪽 아래에 글자 수. */
export function TextArea({
  label,
  error,
  className,
  id,
  maxLength,
  value,
  ...rest
}: TextAreaProps) {
  const autoId = useId();
  const inputId = id ?? autoId;
  const errorId = useId();
  const length = typeof value === "string" ? value.length : 0;
  return (
    <div className={className}>
      <label htmlFor={inputId} className="mb-2 block text-caption text-text-sub">
        {label}
      </label>
      <textarea
        id={inputId}
        value={value}
        maxLength={maxLength}
        aria-invalid={!!error || undefined}
        aria-describedby={error ? errorId : undefined}
        className={clsx(
          "min-h-32 w-full resize-none rounded-md bg-bg-muted px-4 py-3 text-body1 text-text-strong outline-none placeholder:text-text-hint",
          "focus:ring-2 focus:ring-accent",
          error && "ring-2 ring-danger",
        )}
        {...rest}
      />
      <div className="mt-2 flex gap-2">
        {error && (
          <p id={errorId} className="flex-1 text-caption text-danger">
            {error}
          </p>
        )}
        {maxLength !== undefined && (
          <p className="ml-auto text-caption text-text-hint">
            {length}/{maxLength}
          </p>
        )}
      </div>
    </div>
  );
}
