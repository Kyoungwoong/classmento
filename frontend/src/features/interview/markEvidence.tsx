import type { ReactNode } from "react";

/** 노랑은 한 화면 세 군데까지라(design-system 원칙 2) 형광펜은 두 군데까지 */
const MAX_MARKS = 2;

/**
 * 답변 글자 안에서 채점 근거로 인용된 부분을 형광펜(`<mark>`)으로 칠한다.
 * 근거가 답변에 그대로 없으면(요약 인용 등) 칠하지 않는다. 겹치면 먼저 나온 것을 쓴다.
 * 근거 순서대로 두 군데(MAX_MARKS)까지만 칠한다.
 */

export function markEvidence(
  text: string,
  evidences: readonly (string | null | undefined)[],
): ReactNode[] {
  const ranges: [number, number][] = [];
  for (const e of evidences) {
    const q = e?.trim();
    if (!q) continue;
    const at = text.indexOf(q);
    if (at < 0) continue;
    const end = at + q.length;
    if (ranges.some(([s, t]) => at < t && s < end)) continue;
    ranges.push([at, end]);
    if (ranges.length === MAX_MARKS) break;
  }
  ranges.sort((a, b) => a[0] - b[0]);
  const out: ReactNode[] = [];
  let i = 0;
  ranges.forEach(([s, t], k) => {
    if (s > i) out.push(text.slice(i, s));
    out.push(
      <mark key={k} className="rounded-sm bg-highlight-weak px-0.5 text-text">
        {text.slice(s, t)}
      </mark>,
    );
    i = t;
  });
  if (i < text.length) out.push(text.slice(i));
  return out;
}
