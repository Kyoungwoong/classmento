import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Mascot } from "./Mascot";

describe("Mascot", () => {
  it("이름을 주면 그림으로 읽히고 표정을 남긴다", () => {
    render(<Mascot mood="listening" label="듣고 있는 멘토리" />);
    const img = screen.getByRole("img", { name: "듣고 있는 멘토리" });
    expect(img).toHaveAttribute("data-mood", "listening");
  });

  it("이름이 없으면 장식이라 스크린리더가 건너뛴다", () => {
    const { container } = render(<Mascot mood="happy" />);
    expect(screen.queryByRole("img")).not.toBeInTheDocument();
    expect(container.querySelector("svg")).toHaveAttribute("aria-hidden", "true");
  });
});
