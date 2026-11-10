import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { UploadScreen } from "./UploadScreen";

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn(), replace: vi.fn() }) }));

function drop(target: Element, file: File) {
  fireEvent.dragOver(target, { dataTransfer: { files: [file], types: ["Files"] } });
  fireEvent.drop(target, { dataTransfer: { files: [file], types: ["Files"] } });
}

describe("UploadScreen", () => {
  it("파일을 끌어다 놓으면 고른 파일로 보여 주고 올리기를 켠다", () => {
    render(<UploadScreen />);
    const zone = screen.getByRole("button", { name: /파일 고르기/ });
    drop(zone, new File(["x"], "me.pdf", { type: "application/pdf" }));
    expect(screen.getByText("me.pdf")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "올리기" })).toBeEnabled();
  });

  it("PDF·DOCX가 아니면 놓아도 고르지 않고 이유를 알려 준다", () => {
    render(<UploadScreen />);
    drop(screen.getByRole("button", { name: /파일 고르기/ }), new File(["x"], "me.txt"));
    expect(screen.getByRole("alert")).toHaveTextContent("PDF나 DOCX 파일만 올릴 수 있어요.");
    expect(screen.getByRole("button", { name: "올리기" })).toBeDisabled();
  });
});
