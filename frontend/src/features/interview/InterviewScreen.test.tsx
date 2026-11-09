import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { api, ApiError, type InterviewView } from "@/lib/api/client";
import { InterviewScreen } from "./InterviewScreen";

const push = vi.fn();
const replace = vi.fn();
// 실제 라우터처럼 렌더마다 같은 객체
const router = { push, replace, back: vi.fn() };
vi.mock("next/navigation", () => ({ useRouter: () => router }));

/** 녹음 훅은 상태만 흉내 낸다(실제 녹음은 useRecorder.test) */
const recorder = {
  state: "idle" as string,
  elapsed: 0,
  blob: null as Blob | null,
  url: null as string | null,
  start: vi.fn(),
  stop: vi.fn(),
  reset: vi.fn(),
};
vi.mock("./useRecorder", () => ({ MAX_SECONDS: 180, useRecorder: () => recorder }));

function interview(answered: number[] = []): InterviewView {
  return {
    id: 7,
    type: "TECH",
    status: "IN_PROGRESS",
    createdAt: "2026-11-09T00:00:00Z",
    questions: [1, 2, 3].map((seq) => ({
      seq,
      text: `질문 ${seq}번이에요`,
      kind: "TECH",
      answerStatus: answered.includes(seq) ? "EVALUATED" : undefined,
    })),
  };
}

beforeEach(() => {
  Object.assign(recorder, { state: "idle", elapsed: 0, blob: null, url: null });
  push.mockReset();
  replace.mockReset();
});
afterEach(() => vi.restoreAllMocks());

describe("InterviewScreen", () => {
  it("아직 답하지 않은 첫 질문과 진행도를 보여준다", async () => {
    vi.spyOn(api, "interview").mockResolvedValue(interview([1]));
    render(<InterviewScreen id={7} />);
    expect(await screen.findByRole("heading", { name: "질문 2번이에요" })).toBeInTheDocument();
    expect(screen.getByText("2 / 3")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "답변 시작하기" }));
    expect(recorder.start).toHaveBeenCalled();
  });

  it("녹음 중에는 경과 시간과 [답변 끝내기]", async () => {
    vi.spyOn(api, "interview").mockResolvedValue(interview());
    Object.assign(recorder, { state: "recording", elapsed: 83 });
    render(<InterviewScreen id={7} />);
    expect(await screen.findByText("01:23")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "답변 끝내기" }));
    expect(recorder.stop).toHaveBeenCalled();
  });

  it("다 녹음하면 올리고 다음 질문으로", async () => {
    vi.spyOn(api, "interview").mockResolvedValue(interview());
    const upload = vi.spyOn(api, "uploadAnswer").mockResolvedValue({ seq: 1, status: "UPLOADED" });
    const blob = new Blob(["v"], { type: "audio/webm" });
    Object.assign(recorder, { state: "recorded", blob, url: "blob:x" });
    render(<InterviewScreen id={7} />);

    fireEvent.click(await screen.findByRole("button", { name: "다음 질문" }));

    await waitFor(() => expect(upload).toHaveBeenCalledWith(7, 1, blob));
    expect(await screen.findByRole("heading", { name: "질문 2번이에요" })).toBeInTheDocument();
    expect(recorder.reset).toHaveBeenCalled();
  });

  it("마지막 질문이면 올리고 끝낸 뒤 리포트로", async () => {
    vi.spyOn(api, "interview").mockResolvedValue(interview([1, 2]));
    vi.spyOn(api, "uploadAnswer").mockResolvedValue({ seq: 3, status: "UPLOADED" });
    const finish = vi.spyOn(api, "finishInterview").mockResolvedValue(interview([1, 2, 3]));
    Object.assign(recorder, { state: "recorded", blob: new Blob(["v"]), url: "blob:x" });
    render(<InterviewScreen id={7} />);

    fireEvent.click(await screen.findByRole("button", { name: "면접 끝내기" }));

    await waitFor(() => expect(finish).toHaveBeenCalledWith(7));
    expect(push).toHaveBeenCalledWith("/interview/7/report");
  });

  it("끝내기만 실패하면 다시 올리지 않고 끝내기만 다시 한다", async () => {
    vi.spyOn(api, "interview").mockResolvedValue(interview([1, 2]));
    const upload = vi.spyOn(api, "uploadAnswer").mockResolvedValue({ seq: 3, status: "UPLOADED" });
    const finish = vi
      .spyOn(api, "finishInterview")
      .mockRejectedValueOnce(new Error("network"))
      .mockResolvedValueOnce(interview([1, 2, 3]));
    Object.assign(recorder, { state: "recorded", blob: new Blob(["v"]), url: "blob:x" });
    render(<InterviewScreen id={7} />);

    fireEvent.click(await screen.findByRole("button", { name: "면접 끝내기" }));
    fireEvent.click(await screen.findByRole("button", { name: "다시 끝내기" }));

    await waitFor(() => expect(push).toHaveBeenCalledWith("/interview/7/report"));
    expect(upload).toHaveBeenCalledTimes(1);
    expect(finish).toHaveBeenCalledTimes(2);
  });

  it("불러오지 못하면 [다시 불러오기]로 다시 시도한다", async () => {
    const get = vi
      .spyOn(api, "interview")
      .mockRejectedValueOnce(new ApiError(500, "INTERNAL", "x"))
      .mockResolvedValueOnce(interview());
    render(<InterviewScreen id={7} />);

    fireEvent.click(await screen.findByRole("button", { name: "다시 불러오기" }));

    expect(await screen.findByRole("heading", { name: "질문 1번이에요" })).toBeInTheDocument();
    expect(get).toHaveBeenCalledTimes(2);
  });

  it("없는 면접이면 새 면접 만들기로 안내한다", async () => {
    vi.spyOn(api, "interview").mockRejectedValue(new ApiError(404, "NOT_FOUND", "x"));
    render(<InterviewScreen id={7} />);
    expect(await screen.findByRole("link", { name: "새 면접 만들기" })).toHaveAttribute(
      "href",
      "/interview/new",
    );
  });

  it("모두 올린 뒤 다시 열면 다시 녹음하지 않고 바로 끝낼 수 있다", async () => {
    vi.spyOn(api, "interview").mockResolvedValue(interview([1, 2, 3]));
    const upload = vi.spyOn(api, "uploadAnswer");
    const finish = vi.spyOn(api, "finishInterview").mockResolvedValue(interview([1, 2, 3]));
    render(<InterviewScreen id={7} />);

    fireEvent.click(await screen.findByRole("button", { name: "면접 끝내기" }));

    await waitFor(() => expect(finish).toHaveBeenCalledWith(7));
    expect(upload).not.toHaveBeenCalled();
    expect(push).toHaveBeenCalledWith("/interview/7/report");
  });

  it("아직 정리 중이면 잠시 뒤 다시 끝낸다", async () => {
    vi.spyOn(api, "interview").mockResolvedValue(interview([1, 2, 3]));
    const finish = vi
      .spyOn(api, "finishInterview")
      .mockRejectedValueOnce(new ApiError(409, "ANSWERS_PROCESSING", "x"))
      .mockResolvedValueOnce(interview([1, 2, 3]));
    render(<InterviewScreen id={7} />);

    fireEvent.click(await screen.findByRole("button", { name: "면접 끝내기" }));

    await waitFor(() => expect(push).toHaveBeenCalledWith("/interview/7/report"), {
      timeout: 4000,
    });
    expect(finish).toHaveBeenCalledTimes(2);
  });

  it("실패한 답변을 다시 불러오지 못해도 멈추지 않고 [다시 끝내기]", async () => {
    vi.spyOn(api, "interview")
      .mockResolvedValueOnce(interview([1, 2, 3]))
      .mockRejectedValueOnce(new Error("network"));
    vi.spyOn(api, "finishInterview").mockRejectedValue(new ApiError(409, "ANSWERS_FAILED", "x"));
    render(<InterviewScreen id={7} />);

    fireEvent.click(await screen.findByRole("button", { name: "면접 끝내기" }));

    expect(await screen.findByRole("button", { name: "다시 끝내기" })).toBeEnabled();
  });

  it("올리기 실패 뒤 다시 녹음하면 [다시 올리기]가 아니라 원래 버튼", async () => {
    vi.spyOn(api, "interview").mockResolvedValue(interview());
    vi.spyOn(api, "uploadAnswer").mockRejectedValue(new Error("network"));
    Object.assign(recorder, { state: "recorded", blob: new Blob(["v"]), url: "blob:x" });
    render(<InterviewScreen id={7} />);

    fireEvent.click(await screen.findByRole("button", { name: "다음 질문" }));
    fireEvent.click(await screen.findByRole("button", { name: "다시 녹음" }));

    expect(await screen.findByRole("button", { name: "다음 질문" })).toBeInTheDocument();
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
  });

  it("숫자가 아닌 주소면 부르지 않고 없는 면접으로 안내한다", async () => {
    const get = vi.spyOn(api, "interview");
    render(<InterviewScreen id={NaN} />);
    expect(await screen.findByRole("link", { name: "새 면접 만들기" })).toBeInTheDocument();
    expect(get).not.toHaveBeenCalled();
  });

  it("다시 녹음해야 하는 답변이 있으면 그 질문으로 돌아간다", async () => {
    const failed = interview([1, 2, 3]);
    failed.questions[1] = {
      ...failed.questions[1],
      answerStatus: "FAILED",
      failureReason: "답변이 들리지 않았어요. 다시 녹음해 주세요.",
    };
    vi.spyOn(api, "interview")
      .mockResolvedValueOnce(interview([1, 2]))
      .mockResolvedValue(failed);
    vi.spyOn(api, "uploadAnswer").mockResolvedValue({ seq: 3, status: "UPLOADED" });
    vi.spyOn(api, "finishInterview").mockRejectedValue(
      new ApiError(409, "ANSWERS_FAILED", "다시 녹음해야 하는 답변이 있어요."),
    );
    Object.assign(recorder, { state: "recorded", blob: new Blob(["v"]), url: "blob:x" });
    render(<InterviewScreen id={7} />);

    fireEvent.click(await screen.findByRole("button", { name: "면접 끝내기" }));

    expect(await screen.findByRole("heading", { name: "질문 2번이에요" })).toBeInTheDocument();
    expect(screen.getByRole("alert")).toHaveTextContent(
      "답변이 들리지 않았어요. 다시 녹음해 주세요.",
    );
  });

  it("올리지 못하면 그 자리에서 다시 올릴 수 있다", async () => {
    vi.spyOn(api, "interview").mockResolvedValue(interview());
    vi.spyOn(api, "uploadAnswer").mockRejectedValue(new Error("network"));
    Object.assign(recorder, { state: "recorded", blob: new Blob(["v"]), url: "blob:x" });
    render(<InterviewScreen id={7} />);
    fireEvent.click(await screen.findByRole("button", { name: "다음 질문" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("답변을 올리지 못했어요");
    expect(screen.getByRole("heading", { name: "질문 1번이에요" })).toBeInTheDocument();
  });

  it("마이크를 쓸 수 없으면 방법을 알려준다", async () => {
    vi.spyOn(api, "interview").mockResolvedValue(interview());
    Object.assign(recorder, { state: "denied" });
    render(<InterviewScreen id={7} />);
    expect(await screen.findByRole("alert")).toHaveTextContent("마이크");
  });

  it("끝난 면접이면 리포트로 보낸다", async () => {
    vi.spyOn(api, "interview").mockResolvedValue({ ...interview([1, 2, 3]), status: "FINISHED" });
    render(<InterviewScreen id={7} />);
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/interview/7/report"));
  });
});
