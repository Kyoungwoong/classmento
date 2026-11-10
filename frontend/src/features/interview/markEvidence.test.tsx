import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { markEvidence } from "./markEvidence";

function marks(text: string, ev: (string | null | undefined)[]) {
  const { container } = render(<p>{markEvidence(text, ev)}</p>);
  return {
    text: container.textContent,
    marked: [...container.querySelectorAll("mark")].map((m) => m.textContent),
  };
}

describe("markEvidence", () => {
  it("근거 인용을 답변 순서대로 칠하고 글자는 그대로 둔다", () => {
    const r = marks("먼저 기준을 정리했고, 결과로 합의했습니다.", ["결과로 합의", "기준을 정리"]);
    expect(r.text).toBe("먼저 기준을 정리했고, 결과로 합의했습니다.");
    expect(r.marked).toEqual(["기준을 정리", "결과로 합의"]);
  });

  it("답변에 없는 근거·빈 근거·겹치는 근거는 건너뛴다", () => {
    const r = marks("기준을 정리했습니다", ["없는 말", null, "  ", "기준을 정리", "정리했"]);
    expect(r.marked).toEqual(["기준을 정리"]);
  });
});
