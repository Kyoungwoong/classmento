import { describe, expect, it } from "vitest";
import { safeNext } from "./safeNext";

describe("safeNext", () => {
  it("내부 경로는 그대로 돌려준다", () => {
    expect(safeNext("/resume/upload")).toBe("/resume/upload");
  });

  it("외부 주소나 비어 있으면 기본값", () => {
    expect(safeNext("https://evil.com")).toBe("/");
    expect(safeNext("//evil.com")).toBe("/");
    expect(safeNext("/\\evil.com")).toBe("/");
    expect(safeNext(null)).toBe("/");
  });
});
