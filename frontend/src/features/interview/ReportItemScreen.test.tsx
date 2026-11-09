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
    expect(screen.getByText("그때 저는 먼저 서로의 기준을 정리했습니다.")).toBeInTheDocument();
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
