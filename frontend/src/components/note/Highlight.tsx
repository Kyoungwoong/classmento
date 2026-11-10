import type { ReactNode } from "react";

/** 노랑 형광펜. 제목의 핵심어 하나에만 칠한다(글자는 그대로 읽힌다). */
export function Highlight({ children }: { children: ReactNode }) {
  return (
    <span className="relative isolate inline-block">
      <span
        aria-hidden="true"
        className="absolute -inset-x-1 top-1/2 bottom-1 -z-10 -rotate-1 rounded-sm bg-highlight"
      />
      {children}
    </span>
  );
}
