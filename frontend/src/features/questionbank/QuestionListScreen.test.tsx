import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { api } from "@/lib/api/client";
import { QuestionListScreen } from "./QuestionListScreen";
import { question, tree } from "./testData";

const push = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push, replace: vi.fn(), back: vi.fn() }),
}));

afterEach(() => vi.restoreAllMocks());

function setup() {
  vi.spyOn(api, "adminCategories").mockResolvedValue(tree);
  return vi
    .spyOn(api, "adminQuestions")
    .mockResolvedValue({ items: [question()], total: 1, page: 0, size: 20 });
}

describe("QuestionListScreen", () => {
  it("질문 수와 목록(소분류 · 난이도)을 보여주고, 누르면 수정 화면으로", async () => {
    setup();
    render(<QuestionListScreen />);
    expect(await screen.findByRole("heading", { name: "질문이 1개 있어요" })).toBeInTheDocument();
    const row = screen.getByRole("button", { name: /TCP와 UDP/ });
    expect(row).toHaveTextContent("네트워크 · 신입");
    fireEvent.click(row);
    expect(push).toHaveBeenCalledWith("/admin/questions/100");
  });

  it("대분류를 고르면 소분류가 펼쳐지고, 고른 조건으로 다시 찾는다", async () => {
    const load = setup();
    render(<QuestionListScreen />);
    fireEvent.click(await screen.findByRole("button", { name: "CS" }));
    await waitFor(() =>
      expect(load).toHaveBeenLastCalledWith(expect.objectContaining({ topCategoryId: 1 })),
    );
    fireEvent.click(screen.getByRole("button", { name: "보안" }));
    await waitFor(() =>
      expect(load).toHaveBeenLastCalledWith(
        expect.objectContaining({ topCategoryId: 1, categoryId: 12 }),
      ),
    );
    fireEvent.click(screen.getByRole("radio", { name: "주니어" }));
    await waitFor(() =>
      expect(load).toHaveBeenLastCalledWith(expect.objectContaining({ level: "JUNIOR" })),
    );
    fireEvent.click(screen.getByRole("switch", { name: "숨긴 질문도 보기" }));
    await waitFor(() =>
      expect(load).toHaveBeenLastCalledWith(expect.objectContaining({ includeHidden: true })),
    );
  });

  it("조건에 맞는 질문이 없으면 알려준다", async () => {
    vi.spyOn(api, "adminCategories").mockResolvedValue(tree);
    vi.spyOn(api, "adminQuestions").mockResolvedValue({ items: [], total: 0, page: 0, size: 20 });
    render(<QuestionListScreen />);
    expect(await screen.findByText("조건에 맞는 질문이 없어요")).toBeInTheDocument();
  });

  it("불러오기 실패 → 다시 시도", async () => {
    vi.spyOn(api, "adminCategories").mockResolvedValue(tree);
    const load = vi
      .spyOn(api, "adminQuestions")
      .mockRejectedValueOnce(new Error("x"))
      .mockResolvedValue({ items: [question()], total: 1, page: 0, size: 20 });
    render(<QuestionListScreen />);
    expect(await screen.findByRole("alert")).toHaveTextContent("질문을 불러오지 못했어요.");
    fireEvent.click(screen.getByRole("button", { name: "다시 시도하기" }));
    expect(await screen.findByRole("button", { name: /TCP와 UDP/ })).toBeInTheDocument();
    expect(load).toHaveBeenCalledTimes(2);
  });
});
