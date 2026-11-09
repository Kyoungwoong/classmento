import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SegmentedControl } from "./SegmentedControl";

const options = [
  { value: "NEW", label: "신입" },
  { value: "JUNIOR", label: "주니어" },
] as const;

describe("SegmentedControl", () => {
  it("선택된 항목은 aria-checked, 누르면 onChange", () => {
    const onChange = vi.fn();
    render(<SegmentedControl label="난이도" options={options} value="NEW" onChange={onChange} />);
    expect(screen.getByRole("radiogroup", { name: "난이도" })).toBeInTheDocument();
    expect(screen.getByRole("radio", { name: "신입" })).toHaveAttribute("aria-checked", "true");
    fireEvent.click(screen.getByRole("radio", { name: "주니어" }));
    expect(onChange).toHaveBeenCalledWith("JUNIOR");
  });
});
