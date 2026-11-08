import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Button } from "./Button";

describe("Button", () => {
  it("로딩 중에는 비활성화되고 안내 문구를 보여준다", () => {
    render(<Button loading>제출하기</Button>);
    const button = screen.getByRole("button");
    expect(button).toBeDisabled();
    expect(button).toHaveTextContent("잠시만 기다려 주세요");
  });

  it("기본은 primary 큰 버튼이다", () => {
    render(<Button>다음</Button>);
    expect(screen.getByRole("button", { name: "다음" })).toHaveClass("bg-primary", "h-14");
  });
});
