import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { api, ApiError } from "@/lib/api/client";
import { QuestionFormScreen } from "./QuestionFormScreen";
import { question, tree } from "./testData";

const replace = vi.fn();
// 실제 라우터처럼 렌더마다 같은 객체
const router = { push: vi.fn(), replace, back: vi.fn() };
vi.mock("next/navigation", () => ({ useRouter: () => router }));

afterEach(() => vi.restoreAllMocks());

describe("QuestionFormScreen", () => {
  it("빈 채로 저장하면 칸마다 할 일을 알려주고 API를 부르지 않는다", async () => {
    vi.spyOn(api, "adminCategories").mockResolvedValue(tree);
    const create = vi.spyOn(api, "createQuestion");
    render(<QuestionFormScreen />);
    fireEvent.click(await screen.findByRole("button", { name: "저장하기" }));
    // 대분류도 안 골랐으면 대분류 묶음에 알리고 첫 칩으로 포커스
    expect(screen.getByRole("group", { name: "대분류" })).toHaveAccessibleDescription(
      "대분류와 소분류를 골라 주세요.",
    );
    expect(screen.getByRole("button", { name: "CS" })).toHaveFocus();
    expect(screen.getByText("질문은 5~300자로 입력해 주세요.")).toBeInTheDocument();
    expect(screen.getByText("난이도를 골라 주세요.")).toBeInTheDocument();
    expect(screen.getByText("직무를 하나 이상 골라 주세요.")).toBeInTheDocument();
    // 칩 묶음은 라벨·에러와 이어져 있다(스크린리더)
    expect(screen.getByLabelText("질문")).toHaveAccessibleDescription(
      "질문은 5~300자로 입력해 주세요.",
    );
    expect(screen.getByRole("radiogroup", { name: "난이도" })).toHaveAccessibleDescription(
      "난이도를 골라 주세요.",
    );
    expect(screen.getByRole("group", { name: "직무" })).toHaveAccessibleDescription(
      "직무를 하나 이상 골라 주세요.",
    );
    expect(create).not.toHaveBeenCalled();
  });

  it("대분류만 고르고 저장하면 소분류 묶음에 알린다", async () => {
    vi.spyOn(api, "adminCategories").mockResolvedValue(tree);
    render(<QuestionFormScreen />);
    fireEvent.click(await screen.findByRole("button", { name: "CS" }));
    fireEvent.click(screen.getByRole("button", { name: "저장하기" }));
    expect(screen.getByRole("group", { name: "소분류" })).toHaveAccessibleDescription(
      "소분류를 골라 주세요.",
    );
    expect(screen.getByRole("button", { name: "네트워크" })).toHaveFocus();
  });

  it("대분류 → 소분류, 질문·난이도·직무를 고르고 저장하면 목록으로 돌아간다", async () => {
    vi.spyOn(api, "adminCategories").mockResolvedValue(tree);
    const create = vi.spyOn(api, "createQuestion").mockResolvedValue(question());
    render(<QuestionFormScreen />);

    // 숨긴 소분류는 고를 수 없다
    fireEvent.click(await screen.findByRole("button", { name: "인성" }));
    expect(screen.queryByRole("button", { name: "갈등" })).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "CS" }));
    fireEvent.click(screen.getByRole("button", { name: "네트워크" }));
    fireEvent.change(screen.getByLabelText("질문"), {
      target: { value: "  TCP와 UDP의 차이를 설명해 주세요.  " },
    });
    fireEvent.click(screen.getByRole("radio", { name: "신입" }));
    fireEvent.click(screen.getByRole("button", { name: "공통" }));
    fireEvent.click(screen.getByRole("button", { name: "저장하기" }));

    await waitFor(() =>
      expect(create).toHaveBeenCalledWith({
        categoryId: 11,
        text: "TCP와 UDP의 차이를 설명해 주세요.",
        level: "NEW",
        jobRoles: ["COMMON"],
        tagIds: [],
        intent: undefined,
      }),
    );
    expect(replace).toHaveBeenCalledWith("/admin/questions");
  });

  it("수정 화면은 기존 값을 채우고, 서버 규칙 오류는 폼 아래에 보여준다", async () => {
    vi.spyOn(api, "adminCategories").mockResolvedValue(tree);
    vi.spyOn(api, "adminQuestion").mockResolvedValue(
      question({ tags: [{ id: 5, name: "TCP" }], intent: "기본기" }),
    );
    vi.spyOn(api, "updateQuestion").mockRejectedValue(
      new ApiError(409, "CATEGORY_HIDDEN", "숨긴 카테고리에는 질문을 넣을 수 없어요."),
    );
    render(<QuestionFormScreen id={100} />);

    expect(await screen.findByLabelText("질문")).toHaveValue("TCP와 UDP의 차이를 설명해 주세요.");
    expect(screen.getByRole("button", { name: "네트워크" })).toHaveAttribute(
      "aria-pressed",
      "true",
    );
    expect(screen.getByRole("button", { name: "TCP 지우기" })).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "저장하기" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "숨긴 카테고리에는 질문을 넣을 수 없어요.",
    );
  });

  it("수정 화면에서 숨기기", async () => {
    vi.spyOn(api, "adminCategories").mockResolvedValue(tree);
    vi.spyOn(api, "adminQuestion").mockResolvedValue(question());
    const setActive = vi
      .spyOn(api, "setQuestionActive")
      .mockResolvedValue(question({ active: false }));
    render(<QuestionFormScreen id={100} />);
    fireEvent.click(await screen.findByRole("button", { name: "숨기기" }));
    await waitFor(() => expect(setActive).toHaveBeenCalledWith(100, false));
    expect(replace).toHaveBeenCalledWith("/admin/questions");
  });
});
