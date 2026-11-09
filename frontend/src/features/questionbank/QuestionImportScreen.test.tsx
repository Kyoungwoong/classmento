import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { api, ApiError } from "@/lib/api/client";
import { QuestionImportScreen } from "./QuestionImportScreen";
import { SAMPLE_CSV } from "./sampleCsv";

// 실제 라우터처럼 렌더마다 같은 객체
const router = { push: vi.fn(), replace: vi.fn(), back: vi.fn() };
vi.mock("next/navigation", () => ({ useRouter: () => router }));

afterEach(() => vi.restoreAllMocks());

function csv(name = "questions.csv", size?: number) {
  const file = new File(["대분류,소분류,질문,난이도,직무,태그,의도\n"], name, { type: "text/csv" });
  if (size !== undefined) Object.defineProperty(file, "size", { value: size });
  return file;
}

function choose(file: File) {
  fireEvent.change(screen.getByLabelText("CSV 파일 고르기"), { target: { files: [file] } });
}

describe("QuestionImportScreen", () => {
  it("파일을 고르기 전에는 올릴 수 없다", () => {
    render(<QuestionImportScreen />);
    expect(screen.getByRole("button", { name: "올리기" })).toBeDisabled();
  });

  it("올리면 몇 개를 넣었는지 알려주고 목록으로 갈 수 있다", async () => {
    const upload = vi.spyOn(api, "importQuestions").mockResolvedValue({ imported: 2, errors: [] });
    render(<QuestionImportScreen />);
    choose(csv());
    expect(screen.getByText("questions.csv")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "올리기" }));

    expect(await screen.findByRole("heading", { name: "질문 2개를 넣었어요" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "목록으로 가기" })).toHaveAttribute(
      "href",
      "/admin/questions",
    );
    expect(upload).toHaveBeenCalledTimes(1);
  });

  it("줄별 오류가 있으면 하나도 저장하지 않았다고 알리고 줄마다 보여준다", async () => {
    vi.spyOn(api, "importQuestions").mockResolvedValue({
      imported: 0,
      errors: [
        { line: 3, message: "없는 카테고리예요: CS > 양자컴퓨팅" },
        { line: 7, message: "질문은 5~300자로 입력해 주세요." },
      ],
    });
    render(<QuestionImportScreen />);
    choose(csv());
    fireEvent.click(screen.getByRole("button", { name: "올리기" }));

    expect(await screen.findByRole("heading", { name: "2줄을 고쳐 주세요" })).toBeInTheDocument();
    expect(
      screen.getByText("아무것도 저장하지 않았어요. 고친 뒤 다시 올려 주세요."),
    ).toBeInTheDocument();
    // 화면이 바뀐 것을 스크린리더가 바로 읽도록 오류 목록은 alert 영역 안에
    expect(screen.getByRole("alert")).toHaveTextContent("3번째 줄");
    expect(screen.getByText("없는 카테고리예요: CS > 양자컴퓨팅")).toBeInTheDocument();
    expect(screen.getByText("7번째 줄")).toBeInTheDocument();
  });

  it("오류 화면에서 [다른 파일 고르기]를 누르면 파일 고르기 창을 연다", async () => {
    vi.spyOn(api, "importQuestions").mockResolvedValue({
      imported: 0,
      errors: [{ line: 2, message: "질문은 5~300자로 입력해 주세요." }],
    });
    const open = vi.spyOn(HTMLInputElement.prototype, "click").mockImplementation(() => {});
    render(<QuestionImportScreen />);
    choose(csv());
    fireEvent.click(screen.getByRole("button", { name: "올리기" }));
    fireEvent.click(await screen.findByRole("button", { name: "다른 파일 고르기" }));
    expect(open).toHaveBeenCalledTimes(1);
  });

  it("한 줄에 오류가 여러 개여도 줄 수로 센다", async () => {
    vi.spyOn(api, "importQuestions").mockResolvedValue({
      imported: 0,
      errors: [
        { line: 2, message: "큰따옴표가 닫히지 않았어요." },
        { line: 2, message: "없는 카테고리예요: CS > 양자컴퓨팅" },
      ],
    });
    render(<QuestionImportScreen />);
    choose(csv());
    fireEvent.click(screen.getByRole("button", { name: "올리기" }));
    expect(await screen.findByRole("heading", { name: "1줄을 고쳐 주세요" })).toBeInTheDocument();
  });

  it("2MB가 넘거나 CSV가 아니면 올리기 전에 알려준다", () => {
    const upload = vi.spyOn(api, "importQuestions");
    render(<QuestionImportScreen />);
    choose(csv("big.csv", 2 * 1024 * 1024 + 1));
    expect(screen.getByText("CSV 파일은 2MB까지 올릴 수 있어요.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "올리기" })).toBeDisabled();

    choose(csv("questions.xlsx"));
    expect(screen.getByText("CSV 파일(.csv)을 골라 주세요.")).toBeInTheDocument();
    expect(upload).not.toHaveBeenCalled();
  });

  it("서버가 파일을 거절하면 원인을 보여준다", async () => {
    vi.spyOn(api, "importQuestions").mockRejectedValue(
      new ApiError(400, "INVALID_REQUEST", "UTF-8로 저장한 CSV를 올려 주세요."),
    );
    render(<QuestionImportScreen />);
    choose(csv());
    fireEvent.click(screen.getByRole("button", { name: "올리기" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("UTF-8로 저장한 CSV를 올려 주세요.");
    await waitFor(() => expect(screen.getByRole("button", { name: "올리기" })).toBeEnabled());
  });

  it("예시 파일은 헤더로 시작한다", () => {
    expect(SAMPLE_CSV.split("\n")[0]).toBe("대분류,소분류,질문,난이도,직무,태그,의도");
  });
});
