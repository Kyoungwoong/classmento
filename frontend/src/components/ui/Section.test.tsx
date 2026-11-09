import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { SectionHeading } from "./Section";

describe("SectionHeading", () => {
  it("남색 띠 위에서는 대비를 지키는 밝은 글자색을 쓴다", () => {
    render(<SectionHeading tone="ink" eyebrow="흐름" title="제목" description="설명" />);
    expect(screen.getByText("흐름")).toHaveClass("text-primary");
    expect(screen.getByRole("heading", { name: "제목" })).toHaveClass("text-on-ink");
    expect(screen.getByText("설명")).toHaveClass("text-navy-100");
  });

  it("흰·연한 띠 위에서는 진한 연두 라벨과 남색 제목", () => {
    render(<SectionHeading tone="tint" eyebrow="흐름" title="제목" description="설명" />);
    expect(screen.getByText("흐름")).toHaveClass("text-accent");
    expect(screen.getByRole("heading", { name: "제목" })).toHaveClass("text-text-strong");
    expect(screen.getByText("설명")).toHaveClass("text-text");
  });
});
