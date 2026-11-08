import clsx from "clsx";
import type { ButtonHTMLAttributes } from "react";

type Variant = "primary" | "secondary" | "ghost" | "danger";
type Size = "lg" | "md" | "sm";

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  fullWidth?: boolean;
  loading?: boolean;
}

const variantClass: Record<Variant, string> = {
  primary: "bg-primary text-white active:bg-primary-pressed",
  secondary: "bg-primary-weak text-primary active:bg-blue-100",
  ghost: "bg-transparent text-text-sub active:bg-bg-muted",
  danger: "bg-danger text-white",
};

const sizeClass: Record<Size, string> = {
  lg: "h-14 px-5 text-body1 font-semibold rounded-md",
  md: "h-12 px-4 text-body2 font-semibold rounded-md",
  sm: "h-9 px-3 text-caption font-medium rounded-sm",
};

export function Button({
  variant = "primary",
  size = "lg",
  fullWidth = false,
  loading = false,
  disabled,
  className,
  children,
  ...rest
}: ButtonProps) {
  return (
    <button
      type="button"
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={clsx(
        "inline-flex items-center justify-center transition-colors duration-100 select-none",
        "disabled:bg-bg-muted disabled:text-text-hint",
        variantClass[variant],
        sizeClass[size],
        fullWidth && "w-full",
        className,
      )}
      {...rest}
    >
      {loading ? "잠시만 기다려 주세요" : children}
    </button>
  );
}
