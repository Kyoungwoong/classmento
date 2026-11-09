import clsx from "clsx";
import type { ReactNode } from "react";

interface TwoColumnProps {
  /** 왼쪽(모바일에서는 위): 제목·요약·안내 */
  aside: ReactNode;
  /** 오른쪽(모바일에서는 아래): 실제로 하는 일·목록 */
  children: ReactNode;
  /** 스크롤해도 왼쪽을 붙잡아 둔다(요약 점수 등) */
  stickyAside?: boolean;
  /** 왼쪽 비율: narrow 5/12(기본) · even 6/12 */
  ratio?: "narrow" | "even";
  className?: string;
}

/** 데스크톱 2단(왼쪽 aside · 오른쪽 본문), 모바일 1단(위아래로 쌓기). */
export function TwoColumn({
  aside,
  children,
  stickyAside = false,
  ratio = "narrow",
  className,
}: TwoColumnProps) {
  return (
    <div className={clsx("grid gap-8 md:grid-cols-12 md:gap-12", className)}>
      <div className={clsx(ratio === "even" ? "md:col-span-6" : "md:col-span-5")}>
        <div className={clsx(stickyAside && "md:sticky md:top-24")}>{aside}</div>
      </div>
      <div className={clsx("min-w-0", ratio === "even" ? "md:col-span-6" : "md:col-span-7")}>
        {children}
      </div>
    </div>
  );
}
