import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SelectChip } from "./SelectChip";

describe("SelectChip", () => {
  it("선택 상태를 aria-pressed로 알리고 누르면 onClick", () => {
    const onClick = vi.fn();
    render(<SelectChip label="백엔드" selected onClick={onClick} />);
    const chip = screen.getByRole("button", { name: "백엔드" });
    expect(chip).toHaveAttribute("aria-pressed", "true");
    expect(chip).toHaveClass("bg-primary-weak", "text-primary");
    fireEvent.click(chip);
    expect(onClick).toHaveBeenCalled();
  });

  it("선택 안 됨은 회색", () => {
    render(<SelectChip label="프론트엔드" selected={false} onClick={() => {}} />);
    expect(screen.getByRole("button", { name: "프론트엔드" })).toHaveClass("bg-bg-muted");
  });
});
