import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { api, type ReportView } from "@/lib/api/client";
import { ReportItemScreen } from "./ReportItemScreen";

const router = { push: vi.fn(), replace: vi.fn(), back: vi.fn() };
vi.mock("next/navigation", () => ({ useRouter: () => router }));

afterEach(() => vi.restoreAllMocks());

const view: ReportView = {
  id: 7,
  type: "BEHAVIORAL",
  status: "READY",
  criteria: [],
  items: [
    {
      seq: 2,
      depth: 0,
      question: "팀원과 의견이 달랐던 경험을 말해 주세요.",
      intent: "갈등 조정",
      kind: "BEHAVIORAL",
      answerStatus: "EVALUATED",
      transcript: "그때 저는 먼저 서로의 기준을 정리했습니다.",
      durationMillis: 65000,
      overall: 3.3,
      scores: [
        { criterion: "specificity", score: 4, evidence: "서로의 기준을 정리했습니다" },
        { criterion: "collaboration", score: 2 },
      ],
      strength: "상황을 구체적으로 말했어요.",
      improvement: "결과가 빠졌어요.",
      suggestion: "어떻게 합의했는지로 마무리해 보세요.",
    },
  ],
};

describe("ReportItemScreen", () => {
  it("녹음 다시 듣기·내 답변 글자·기준별 점수와 근거·피드백을 보여준다", async () => {
    vi.spyOn(api, "interviewReport").mockResolvedValue(view);
    const { container } = render(<ReportItemScreen id={7} seq={2} />);

    expect(
      await screen.findByRole("heading", { name: "팀원과 의견이 달랐던 경험을 말해 주세요." }),
    ).toBeInTheDocument();
    expect(container.querySelector("audio")).toHaveAttribute(
      "src",
      "/api/interviews/7/questions/2/audio",
    );
    const transcript = screen.getByText(
      (_, el) =>
        el?.tagName === "P" && el.textContent === "그때 저는 먼저 서로의 기준을 정리했습니다.",
    );
    // 채점 근거로 인용된 부분은 답변 안에서 형광펜으로 표시한다
    expect(transcript.querySelector("mark")).toHaveTextContent("서로의 기준을 정리했습니다");
    // 문항 점수는 큰 숫자(도장은 리포트 종합에만)
    expect(screen.getByText("이 문항 3.3점, 5점 만점")).toHaveClass("sr-only");
    expect(screen.getByRole("progressbar", { name: "구체성 4점" })).toBeInTheDocument();
    expect(screen.getByText("“서로의 기준을 정리했습니다”")).toBeInTheDocument();
    expect(screen.getByText("상황을 구체적으로 말했어요.")).toBeInTheDocument();
    expect(screen.getByText("결과가 빠졌어요.")).toBeInTheDocument();
    expect(screen.getByText("어떻게 합의했는지로 마무리해 보세요.")).toBeInTheDocument();
    expect(screen.getByText("이 질문으로 보려는 것: 갈등 조정")).toBeInTheDocument();
  });

  it("없는 문항이면 알려준다", async () => {
    vi.spyOn(api, "interviewReport").mockResolvedValue(view);
    render(<ReportItemScreen id={7} seq={9} />);
    expect(
      await screen.findByRole("heading", { name: "문항을 찾을 수 없어요" }),
    ).toBeInTheDocument();
  });
  it("불러오지 못하면 알리고 다시 불러올 수 있다", async () => {
    const load = vi
      .spyOn(api, "interviewReport")
      .mockRejectedValueOnce(new Error("network"))
      .mockResolvedValue(view);
    render(<ReportItemScreen id={7} seq={2} />);

    expect(await screen.findByRole("alert")).toHaveTextContent("답변을 불러오지 못했어요");
    fireEvent.click(screen.getByRole("button", { name: "다시 불러오기" }));

    expect(
      await screen.findByRole("heading", { name: "팀원과 의견이 달랐던 경험을 말해 주세요." }),
    ).toBeInTheDocument();
    expect(load).toHaveBeenCalledTimes(2);
  });
  it("숫자가 아닌 주소면 부르지 않고 없는 문항으로 안내한다", async () => {
    const load = vi.spyOn(api, "interviewReport");
    render(<ReportItemScreen id={7} seq={NaN} />);
    expect(
      await screen.findByRole("heading", { name: "문항을 찾을 수 없어요" }),
    ).toBeInTheDocument();
    expect(load).not.toHaveBeenCalled();
  });
  it("답하지 않은 문항(서버 null)이면 '답하지 않았어요'", async () => {
    vi.spyOn(api, "interviewReport").mockResolvedValue({
      ...view,
      items: [
        {
          seq: 3,
          depth: 0,
          question: "마지막 질문이에요.",
          kind: "BEHAVIORAL",
          answerStatus: null as never,
          scores: [],
        },
      ],
    });
    const { container } = render(<ReportItemScreen id={7} seq={3} />);
    expect(await screen.findByText("답하지 않았어요.")).toBeInTheDocument();
    expect(container.querySelector("audio")).toBeNull();
  });
});

describe("ReportItemScreen 꼬리질문", () => {
  it("꼬리 문항은 본 질문 번호로 라벨을 달고 앞 질문으로 가는 링크를 준다", async () => {
    vi.spyOn(api, "interviewReport").mockResolvedValue({
      ...view,
      items: [
        { ...view.items[0], seq: 1, depth: 0, question: "캐시를 왜 쓰셨나요?" },
        { ...view.items[0], seq: 4, parentSeq: 1, depth: 1, question: "무효화는요?" },
        { ...view.items[0], seq: 2, depth: 0, question: "인덱스는요?" },
      ],
    });
    render(<ReportItemScreen id={7} seq={4} />);
    expect(await screen.findByText("질문 1의 꼬리 1")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "앞 질문 보기" })).toHaveAttribute(
      "href",
      "/interview/7/report/1",
    );
  });
});
