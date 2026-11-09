import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { api } from "@/lib/api/client";
import { CategoriesScreen } from "./CategoriesScreen";
import { tree } from "./testData";

// 실제 라우터처럼 렌더마다 같은 객체
const router = { push: vi.fn(), replace: vi.fn(), back: vi.fn() };
vi.mock("next/navigation", () => ({ useRouter: () => router }));

afterEach(() => vi.restoreAllMocks());

describe("CategoriesScreen", () => {
  it("대분류별로 소분류와 질문 수, 숨김 여부를 보여준다", async () => {
    vi.spyOn(api, "adminCategories").mockResolvedValue(tree);
    render(<CategoriesScreen />);
    expect(await screen.findByRole("heading", { name: "CS" })).toBeInTheDocument();
    expect(screen.getByText("질문 2개")).toBeInTheDocument();
    expect(screen.getByText("질문 0개 · 숨김")).toBeInTheDocument();
  });

  it("숨기기를 누르면 저장하고 다시 불러온다", async () => {
    const load = vi.spyOn(api, "adminCategories").mockResolvedValue(tree);
    const update = vi.spyOn(api, "updateCategory").mockResolvedValue({
      id: 11,
      parentId: 1,
      name: "네트워크",
      sortOrder: 0,
      active: false,
    });
    render(<CategoriesScreen />);
    fireEvent.click(await screen.findByRole("button", { name: "네트워크 숨기기" }));
    await waitFor(() => expect(update).toHaveBeenCalledWith(11, { active: false }));
    await waitFor(() => expect(load).toHaveBeenCalledTimes(2));
  });

  it("소분류를 추가한다", async () => {
    vi.spyOn(api, "adminCategories").mockResolvedValue(tree);
    const create = vi.spyOn(api, "createCategory").mockResolvedValue({
      id: 13,
      parentId: 1,
      name: "컴퓨터 구조",
      sortOrder: 2,
      active: true,
    });
    render(<CategoriesScreen />);
    fireEvent.click(await screen.findByRole("button", { name: "CS에 소분류 추가" }));
    fireEvent.change(screen.getByLabelText("CS의 새 소분류"), {
      target: { value: "컴퓨터 구조" },
    });
    fireEvent.click(screen.getByRole("button", { name: "추가" }));
    await waitFor(() => expect(create).toHaveBeenCalledWith({ parentId: 1, name: "컴퓨터 구조" }));
  });

  it("이름을 바꾼다", async () => {
    vi.spyOn(api, "adminCategories").mockResolvedValue(tree);
    const update = vi.spyOn(api, "updateCategory").mockResolvedValue({
      id: 12,
      parentId: 1,
      name: "정보보안",
      sortOrder: 1,
      active: true,
    });
    render(<CategoriesScreen />);
    fireEvent.click(await screen.findByRole("button", { name: "보안 이름 바꾸기" }));
    fireEvent.change(screen.getByLabelText("새 이름"), { target: { value: "정보보안" } });
    fireEvent.click(screen.getByRole("button", { name: "저장" }));
    await waitFor(() => expect(update).toHaveBeenCalledWith(12, { name: "정보보안" }));
  });
});
