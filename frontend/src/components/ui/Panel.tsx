import clsx from "clsx";
import type { HTMLAttributes, ReactNode } from "react";

interface PanelProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  /** tint: 연한 연두 면(강조 묶음 하나), plain: 흰 면 + 테두리(기본) */
  tone?: "plain" | "tint";
}

/**
 * 관련된 것을 한 덩어리로 묶는 면. 흰 면 + 1px 테두리 + 둥근 20px, 그림자 없음.
 * 패널 안에 패널을 넣지 않는다. 화면마다 2~3개 이내.
 * `tone="tint"`는 `Section tone="tint"` 띠 안에 두지 않는다(면이 사라진다).
 */
export function Panel({ children, tone = "plain", className, ...rest }: PanelProps) {
  return (
    <div
      className={clsx(
        "rounded-xl p-5 md:p-7",
        tone === "tint" ? "bg-bg-tint" : "border border-border bg-bg",
        className,
      )}
      {...rest}
    >
      {children}
    </div>
  );
}
