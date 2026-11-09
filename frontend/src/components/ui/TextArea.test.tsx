import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { useState } from "react";
import { TextArea } from "./TextArea";

function Controlled() {
  const [v, setV] = useState("");
  return <TextArea label="질문" value={v} onChange={(e) => setV(e.target.value)} maxLength={300} />;
}

describe("TextArea", () => {
  it("라벨로 찾을 수 있고 글자 수를 보여준다", () => {
    render(<Controlled />);
    const box = screen.getByLabelText("질문");
    fireEvent.change(box, { target: { value: "안녕하세요" } });
    expect(screen.getByText("5/300")).toBeInTheDocument();
  });

  it("에러가 있으면 칸 아래에 보여주고 aria-invalid", () => {
    render(<TextArea label="질문" error="5자 이상 입력해 주세요." defaultValue="" />);
    expect(screen.getByLabelText("질문")).toHaveAttribute("aria-invalid", "true");
    expect(screen.getByText("5자 이상 입력해 주세요.")).toBeInTheDocument();
  });
});
