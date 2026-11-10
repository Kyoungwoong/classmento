import clsx from "clsx";
import type { ReactNode } from "react";

/** 손글씨 한마디 메모(주석). 본문·버튼·폼에는 쓰지 않는다. 화면마다 한두 군데. */
export function HandNote({ children, className }: { children: ReactNode; className?: string }) {
  return <span className={clsx("font-hand text-title2 text-accent", className)}>{children}</span>;
}
