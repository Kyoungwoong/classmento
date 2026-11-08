import clsx from "clsx";
import Link, { type LinkProps } from "next/link";
import type { ButtonHTMLAttributes, ReactNode } from "react";

type Variant = "primary" | "secondary" | "ghost" | "danger";
type Size = "lg" | "md" | "sm";

interface StyleProps {
  variant?: Variant;
  size?: Size;
  fullWidth?: boolean;
}

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement>, StyleProps {
  loading?: boolean;
  /** 로딩 중 문구. 지금 하는 일을 말한다(예: "올리고 있어요"). */
  loadingText?: string;
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

function buttonClass({ variant = "primary", size = "lg", fullWidth = false }: StyleProps) {
  return clsx(
    "inline-flex items-center justify-center transition-colors duration-100 select-none",
    "disabled:bg-bg-muted disabled:text-text-hint",
    variantClass[variant],
    sizeClass[size],
    fullWidth && "w-full",
  );
}

export function Button({
  variant,
  size,
  fullWidth,
  loading = false,
  loadingText = "잠시만 기다려 주세요",
  disabled,
  className,
  children,
  type = "button",
  ...rest
}: ButtonProps) {
  return (
    <button
      type={type}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      className={clsx(buttonClass({ variant, size, fullWidth }), className)}
      {...rest}
    >
      {loading ? loadingText : children}
    </button>
  );
}

interface ButtonLinkProps extends LinkProps, StyleProps {
  className?: string;
  children: ReactNode;
}

/** 화면 이동용 버튼. 모양은 Button과 같고 실제로는 링크(<a>)다. */
export function ButtonLink({ variant, size, fullWidth, className, ...rest }: ButtonLinkProps) {
  return <Link className={clsx(buttonClass({ variant, size, fullWidth }), className)} {...rest} />;
}
