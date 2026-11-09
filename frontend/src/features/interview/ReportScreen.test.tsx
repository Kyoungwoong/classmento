import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api, ApiError, type ReportView } from "@/lib/api/client";
import { ReportScreen } from "./ReportScreen";

const push = vi.fn();
// 실제 라우터처럼 렌더마다 같은 객체
const router = { push, replace: vi.fn(), back: vi.fn() };
vi.mock("next/navigation", () => ({ useRouter: () => router }));

function report(overrides: Partial<ReportView> = {}): ReportView {
  return {
    id: 7,
    type: "TECH",
    status: "READY",
    overall: 3.8,
    strength: "흐름이 잘 보였어요.",
    improvement: "결과를 숫자로 말해 보세요.",
    criteria: [
      { criterion: "accuracy", average: 4 },
      { criterion: "depth", average: 3 },
    ],
    items: [
      {
        seq: 1,
        question: "TCP와 UDP의 차이를 설명해 주세요.",
        kind: "TECH",
        answerStatus: "EVALUATED",
        overall: 3.8,
        scores: [],
      },
      {
        seq: 2,
        question: "프로세스와 스레드의 차이는요?",
        kind: "TECH",
        answerStatus: "FAILED",
        failureReason: "답변을 평가하지 못했어요. 다시 시도해 주세요.",
        scores: [],
      },
    ],
    ...overrides,
  };
}

beforeEach(() => push.mockReset());
afterEach(() => {
  vi.useRealTimers();
  vi.restoreAllMocks();
});

describe("ReportScreen", () => {
  it("종합 점수·강점·보완점·기준별 점수·문항별 점수를 보여주고, 문항을 누르면 자세히", async () => {
    vi.spyOn(api, "interviewReport").mockResolvedValue(report());
    render(<ReportScreen id={7} />);

    expect(await screen.findByRole("heading", { name: "면접이 끝났어요" })).toBeInTheDocument();
    expect(screen.getByText("3.8")).toBeInTheDocument();
    expect(screen.getByText("흐름이 잘 보였어요.")).toBeInTheDocument();
    expect(screen.getByText("결과를 숫자로 말해 보세요.")).toBeInTheDocument();
    expect(screen.getByRole("progressbar", { name: "정확성 4.0점" })).toBeInTheDocument();
    expect(screen.getByRole("progressbar", { name: "깊이·근거 3.0점" })).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: /TCP와 UDP/ }));
    expect(push).toHaveBeenCalledWith("/interview/7/report/1");
  });

  it("평가 중이면 안내하고, 끝날 때까지 다시 불러온다", async () => {
    vi.useFakeTimers();
    const load = vi
      .spyOn(api, "interviewReport")
      .mockResolvedValueOnce(
        report({
          status: "EVALUATING",
          overall: undefined,
          strength: undefined,
          improvement: undefined,
        }),
      )
      .mockResolvedValue(report());
    render(<ReportScreen id={7} />);
    await act(async () => {
      await vi.runOnlyPendingTimersAsync();
    });
    expect(screen.getByRole("heading", { name: "답변을 평가하고 있어요" })).toBeInTheDocument();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });
    expect(screen.getByRole("heading", { name: "면접이 끝났어요" })).toBeInTheDocument();
    expect(load).toHaveBeenCalledTimes(2);
  });

  it("실패한 문항은 원인을 보여주고 다시 평가할 수 있다", async () => {
    vi.spyOn(api, "interviewReport").mockResolvedValue(report({ status: "EVALUATING" }));
    const retry = vi.spyOn(api, "retryAnswer").mockResolvedValue({ seq: 2, status: "TRANSCRIBED" });
    render(<ReportScreen id={7} />);

    expect(
      await screen.findByText("답변을 평가하지 못했어요. 다시 시도해 주세요."),
    ).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "2번 다시 평가하기" }));
    await waitFor(() => expect(retry).toHaveBeenCalledWith(7, 2));
  });

  it("[다시 연습하기]는 새 면접으로", async () => {
    vi.spyOn(api, "interviewReport").mockResolvedValue(report());
    render(<ReportScreen id={7} />);
    expect(await screen.findByRole("link", { name: "다시 연습하기" })).toHaveAttribute(
      "href",
      "/interview/new",
    );
  });
  it("다시 평가 요청이 실패하면 그 문항 아래에 알려준다", async () => {
    vi.spyOn(api, "interviewReport").mockResolvedValue(report());
    vi.spyOn(api, "retryAnswer").mockRejectedValue(new Error("network"));
    render(<ReportScreen id={7} />);

    fireEvent.click(await screen.findByRole("button", { name: "2번 다시 평가하기" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("다시 평가를 시작하지 못했어요");
  });

  it("평가 중에 다시 불러오지 못하면 알리고 다시 불러올 수 있다", async () => {
    vi.useFakeTimers();
    const evaluating = report({ status: "EVALUATING", overall: undefined });
    const load = vi
      .spyOn(api, "interviewReport")
      .mockResolvedValueOnce(evaluating)
      .mockRejectedValueOnce(new Error("network"))
      .mockResolvedValue(report());
    render(<ReportScreen id={7} />);
    await act(async () => {
      await vi.runOnlyPendingTimersAsync();
    });
    await act(async () => {
      await vi.advanceTimersByTimeAsync(3000);
    });
    expect(screen.getByRole("alert")).toHaveTextContent("평가 결과를 불러오지 못했어요");

    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "다시 불러오기" }));
    });
    expect(screen.getByRole("heading", { name: "면접이 끝났어요" })).toBeInTheDocument();
    expect(load).toHaveBeenCalledTimes(3);
  });

  it("처음 불러오기가 실패하면 [다시 불러오기], 없는 리포트면 [다시 연습하기]", async () => {
    const load = vi
      .spyOn(api, "interviewReport")
      .mockRejectedValueOnce(new ApiError(500, "INTERNAL", "x"))
      .mockResolvedValue(report());
    render(<ReportScreen id={7} />);

    fireEvent.click(await screen.findByRole("button", { name: "다시 불러오기" }));

    expect(await screen.findByRole("heading", { name: "면접이 끝났어요" })).toBeInTheDocument();
    expect(load).toHaveBeenCalledTimes(2);
  });
});
