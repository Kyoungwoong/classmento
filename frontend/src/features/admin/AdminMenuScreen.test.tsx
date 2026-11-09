import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { api } from "@/lib/api/client";
import { AdminMenuScreen } from "./AdminMenuScreen";

const push = vi.fn();
// 실제 라우터처럼 렌더마다 같은 객체
const router = { push, replace: vi.fn(), back: vi.fn(), refresh: vi.fn() };
vi.mock("next/navigation", () => ({ useRouter: () => router }));

afterEach(() => vi.restoreAllMocks());

describe("AdminMenuScreen", () => {
  it("관리자에게 메뉴를 보여주고 누르면 이동한다", async () => {
    vi.spyOn(api, "me").mockResolvedValue({
      authenticated: true,
      user: { id: 1, email: "a@a.com", name: "관리자", role: "ADMIN" },
    });
    render(<AdminMenuScreen />);
    fireEvent.click(await screen.findByRole("button", { name: /질문 은행/ }));
    expect(push).toHaveBeenCalledWith("/admin/questions");
    expect(screen.getByRole("button", { name: /카테고리/ })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /회원/ })).toBeInTheDocument();
  });

  it("관리자가 아니면 막는다", async () => {
    vi.spyOn(api, "me").mockResolvedValue({
      authenticated: true,
      user: { id: 2, email: "u@u.com", name: "회원", role: "USER" },
    });
    render(<AdminMenuScreen />);
    expect(
      await screen.findByRole("heading", { name: "관리자만 볼 수 있어요." }),
    ).toBeInTheDocument();
  });
});
