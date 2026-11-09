import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { api } from "@/lib/api/client";
import { SiteHeader } from "./SiteHeader";

const replace = vi.fn();
const refresh = vi.fn();
const router = { push: vi.fn(), replace, refresh, back: vi.fn() };
vi.mock("next/navigation", () => ({ useRouter: () => router, usePathname: () => "/" }));

afterEach(() => {
  vi.restoreAllMocks();
  replace.mockReset();
});

describe("SiteHeader", () => {
  it("로고는 홈으로, 메뉴는 면접·이력서로 간다", async () => {
    vi.spyOn(api, "me").mockResolvedValue({ authenticated: false });
    render(<SiteHeader />);
    expect(screen.getByRole("link", { name: "classmento 홈" })).toHaveAttribute("href", "/");
    expect(screen.getByRole("link", { name: "면접 보기" })).toHaveAttribute(
      "href",
      "/interview/new",
    );
    expect(screen.getByRole("link", { name: "이력서 올리기" })).toHaveAttribute(
      "href",
      "/resume/upload",
    );
    expect(await screen.findByRole("link", { name: "로그인" })).toHaveAttribute("href", "/login");
  });

  it("로그인했으면 로그아웃, 관리자면 관리 메뉴", async () => {
    vi.spyOn(api, "me").mockResolvedValue({
      authenticated: true,
      user: { id: 1, email: "a@b.c", role: "ADMIN" } as never,
    });
    const logout = vi.spyOn(api, "logout").mockResolvedValue(undefined);
    render(<SiteHeader />);

    expect(await screen.findByRole("link", { name: "관리" })).toHaveAttribute("href", "/admin");
    fireEvent.click(screen.getByRole("button", { name: "로그아웃" }));

    await waitFor(() => expect(logout).toHaveBeenCalled());
    expect(replace).toHaveBeenCalledWith("/");
    expect(screen.queryByRole("link", { name: "로그인" })).toBeInTheDocument();
  });

  it("모바일 메뉴는 버튼으로 열고 닫는다", async () => {
    vi.spyOn(api, "me").mockResolvedValue({ authenticated: false });
    render(<SiteHeader />);
    const toggle = screen.getByRole("button", { name: "메뉴 열기" });
    expect(toggle).toHaveAttribute("aria-expanded", "false");
    fireEvent.click(toggle);
    expect(screen.getByRole("button", { name: "메뉴 닫기" })).toHaveAttribute(
      "aria-expanded",
      "true",
    );
    expect(screen.getByRole("navigation", { name: "모바일 메뉴" })).toBeInTheDocument();
  });
});
