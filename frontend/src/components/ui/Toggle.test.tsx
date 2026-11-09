import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { Toggle } from "./Toggle";

describe("Toggle", () => {
  it("switch 역할, 누르면 반대 값으로 onChange", () => {
    const onChange = vi.fn();
    render(<Toggle label="숨긴 질문도 보기" checked={false} onChange={onChange} />);
    const sw = screen.getByRole("switch", { name: "숨긴 질문도 보기" });
    expect(sw).toHaveAttribute("aria-checked", "false");
    fireEvent.click(sw);
    expect(onChange).toHaveBeenCalledWith(true);
  });
});
