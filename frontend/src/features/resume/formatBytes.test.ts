import { describe, expect, it } from "vitest";
import { formatBytes, validateResumeFile } from "./formatBytes";

const file = (name: string, size: number) => {
  const f = new File(["x"], name);
  Object.defineProperty(f, "size", { value: size });
  return f;
};

describe("validateResumeFile", () => {
  it("PDF·DOCX 20MB 이하만 통과", () => {
    expect(validateResumeFile(file("이력서.PDF", 1000))).toBeNull();
    expect(validateResumeFile(file("cv.docx", 1000))).toBeNull();
    expect(validateResumeFile(file("cv.hwp", 1000))).toBe("PDF나 DOCX 파일만 올릴 수 있어요.");
    expect(validateResumeFile(file("cv.pdf", 21 * 1024 * 1024))).toContain("20MB");
  });
});

describe("formatBytes", () => {
  it("읽기 쉬운 단위", () => {
    expect(formatBytes(512)).toBe("512B");
    expect(formatBytes(2048)).toBe("2KB");
    expect(formatBytes(3 * 1024 * 1024)).toBe("3.0MB");
  });
});
