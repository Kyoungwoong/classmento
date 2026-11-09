import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { api, ApiError } from "@/lib/api/client";
import { RecentInterviews } from "./RecentInterviews";

afterEach(() => vi.restoreAllMocks());

describe("RecentInterviews", () => {
  it("최근 면접을 보여주고, 진행 중이면 이어서·끝났으면 리포트로", async () => {
    vi.spyOn(api, "interviews").mockResolvedValue([
      {
        id: 2,
        type: "BEHAVIORAL",
        status: "REPORTED",
        questionCount: 3,
        createdAt: "2026-11-09T01:00:00Z",
      },
      {
        id: 1,
        type: "TECH",
        status: "IN_PROGRESS",
        questionCount: 5,
        createdAt: "2026-11-08T01:00:00Z",
      },
    ]);
    render(<RecentInterviews />);
    expect(await screen.findByRole("heading", { name: "최근 면접" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /인성 면접/ })).toHaveAttribute(
      "href",
      "/interview/2/report",
    );
    expect(screen.getByRole("link", { name: /기술 면접/ })).toHaveAttribute("href", "/interview/1");
    expect(screen.getByRole("link", { name: /기술 면접/ })).toHaveTextContent("이어서 하기");
  });

  it("로그인하지 않았거나 면접이 없으면 아무것도 보이지 않는다", async () => {
    vi.spyOn(api, "interviews").mockRejectedValue(new ApiError(401, "UNAUTHORIZED", "로그인"));
    const { container } = render(<RecentInterviews />);
    await new Promise((r) => setTimeout(r, 0));
    expect(container).toBeEmptyDOMElement();
  });
});
