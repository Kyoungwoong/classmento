import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { api, type ResumeDetail } from "@/lib/api/client";
import { ResumeScreen } from "./ResumeScreen";

vi.mock("next/navigation", () => ({
  useParams: () => ({ id: "3" }),
  useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
}));

afterEach(() => vi.restoreAllMocks());

const base = { id: 3, filename: "me.pdf", createdAt: "2026-11-10T00:00:00Z" } as const;

describe("ResumeScreen 상태 그림", () => {
  it("읽는 중이면 생각하는 마스코트와 단계", async () => {
    vi.spyOn(api, "resume").mockResolvedValue({ ...base, status: "UPLOADED" } as ResumeDetail);
    const { container } = render(<ResumeScreen />);
    expect(await screen.findByRole("heading", { name: "이력서를 읽고 있어요" })).toBeInTheDocument();
    expect(container.querySelector("[data-mood]")).toHaveAttribute("data-mood", "thinking");
  });

  it("읽지 못했으면 문제 표정과 원인", async () => {
    vi.spyOn(api, "resume").mockResolvedValue({
      ...base,
      status: "FAILED",
      failureReason: "글자를 찾지 못했어요.",
    } as ResumeDetail);
    const { container } = render(<ResumeScreen />);
    expect(await screen.findByText("글자를 찾지 못했어요.")).toBeInTheDocument();
    expect(container.querySelector("[data-mood]")).toHaveAttribute("data-mood", "oops");
  });
});
