import clsx from "clsx";
import { useId } from "react";

/**
 * 모눈 바탕(장식). 부모에 `relative isolate`를 주고 첫 자식으로 둔다.
 * 그라데이션 대신 SVG 패턴으로 그린다(design-system 금지 목록).
 */
export function GridPaper({ className }: { className?: string }) {
  const id = `grid${useId().replace(/[^a-zA-Z0-9]/g, "")}`;
  return (
    <svg
      aria-hidden="true"
      className={clsx("pointer-events-none absolute inset-0 -z-10 size-full", className)}
    >
      <defs>
        <pattern id={id} width="28" height="28" patternUnits="userSpaceOnUse">
          <path d="M28 0H0V28" fill="none" strokeWidth="1" className="stroke-rule" />
        </pattern>
      </defs>
      <rect width="100%" height="100%" fill={`url(#${id})`} />
    </svg>
  );
}
