import clsx from "clsx";
import { useId, type InputHTMLAttributes } from "react";

interface TextFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

export function TextField({ label, error, className, id, ...rest }: TextFieldProps) {
  const autoId = useId();
  const inputId = id ?? autoId;
  return (
    <div className={className}>
      <label htmlFor={inputId} className="mb-2 block text-caption text-text-sub">
        {label}
      </label>
      <input
        id={inputId}
        aria-invalid={!!error || undefined}
        className={clsx(
          "h-14 w-full rounded-md bg-bg-muted px-4 text-body1 text-text-strong outline-none placeholder:text-text-hint",
          "focus:ring-2 focus:ring-accent",
          error && "ring-2 ring-danger",
        )}
        {...rest}
      />
      {error && <p className="mt-2 text-caption text-danger">{error}</p>}
    </div>
  );
}
