import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { api } from "@/lib/api/client";
import { HomeLanding } from "./HomeLanding";

afterEach(() => vi.restoreAllMocks());

describe("HomeLanding", () => {
  it("히어로 제목과 시작 버튼, 준비 흐름·면접 종류 섹션을 보여준다", () => {
    vi.spyOn(api, "interviews").mockRejectedValue(new Error("401"));
    render(<HomeLanding />);

    expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("연습도 말로 해요");
    const starts = screen.getAllByRole("link", { name: "면접 시작하기" });
    expect(starts[0]).toHaveAttribute("href", "/interview/new");
    expect(screen.getByRole("link", { name: "이력서 올리기" })).toHaveAttribute(
      "href",
      "/resume/upload",
    );
    expect(screen.getByRole("figure", { name: "이력서에서 리포트까지 4단계" })).toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "준비는 단순하게, 연습은 실전처럼" }),
    ).toBeInTheDocument();
    expect(screen.getByText("경험 기반 면접")).toBeInTheDocument();
  });

  it("로그인했고 면접이 있으면 최근 면접을 히어로 아래에 보여준다", async () => {
    vi.spyOn(api, "interviews").mockResolvedValue([
      {
        id: 3,
        type: "TECH",
        status: "IN_PROGRESS",
        questionCount: 5,
        createdAt: "2026-11-09T00:00:00Z",
      } as never,
    ]);
    render(<HomeLanding />);
    expect(await screen.findByRole("heading", { name: "최근 면접" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /기술 면접/ })).toHaveAttribute("href", "/interview/3");
  });
});
