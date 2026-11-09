import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { api, ApiError, type ExperienceView, type InterviewView } from "@/lib/api/client";
import { NewInterviewScreen } from "./NewInterviewScreen";

const push = vi.fn();
// 실제 라우터처럼 렌더마다 같은 객체
const router = { push, replace: vi.fn(), back: vi.fn() };
vi.mock("next/navigation", () => ({ useRouter: () => router }));

afterEach(() => vi.restoreAllMocks());

const created: InterviewView = {
  id: 9,
  type: "TECH",
  status: "IN_PROGRESS",
  questions: [],
  createdAt: "2026-11-09T00:00:00Z",
};

const exp: ExperienceView = {
  id: 31,
  seq: 0,
  type: "PROJECT",
  title: "실시간 재고 동기화",
  summary: "Kafka 이벤트로 재고를 맞춤",
  skills: ["Kafka"],
};

function noResume() {
  vi.spyOn(api, "resumes").mockResolvedValue([]);
}

function parsedResume() {
  vi.spyOn(api, "resumes").mockResolvedValue([
    { id: 3, filename: "me.pdf", status: "PARSED", createdAt: "2026-11-09T00:00:00Z" },
  ]);
  vi.spyOn(api, "resumeExperiences").mockResolvedValue([exp]);
}

const next = () => fireEvent.click(screen.getByRole("button", { name: "다음" }));

describe("NewInterviewScreen", () => {
  it("이력서가 없으면 경험 기반은 고를 수 없다", async () => {
    noResume();
    render(<NewInterviewScreen />);
    const experience = await screen.findByRole("radio", { name: /경험 기반 면접/ });
    await waitFor(() => expect(experience).toBeDisabled());
    expect(experience).toHaveTextContent("이력서가 필요해요");
  });

  it("기술 면접: 직무·연차 → 질문 수 → 만들면 진행 화면으로", async () => {
    noResume();
    const create = vi.spyOn(api, "createInterview").mockResolvedValue(created);
    render(<NewInterviewScreen />);

    fireEvent.click(await screen.findByRole("radio", { name: /기술 면접/ }));
    next();
    expect(screen.getByRole("heading", { name: /어떤 직무/ })).toBeInTheDocument();
    next(); // 아직 고르지 않음
    expect(screen.getByText("직무와 연차를 골라 주세요.")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "백엔드" }));
    fireEvent.click(screen.getByRole("radio", { name: "주니어" }));
    next();
    fireEvent.click(screen.getByRole("radio", { name: "5개" }));
    fireEvent.click(screen.getByRole("button", { name: "질문 만들기" }));

    await waitFor(() =>
      expect(create).toHaveBeenCalledWith({
        type: "TECH",
        count: 5,
        level: "JUNIOR",
        jobRole: "BACKEND",
        narrowByResume: false,
      }),
    );
    expect(push).toHaveBeenCalledWith("/interview/9");
  });

  it("경험 기반: 경험을 골라 만든다", async () => {
    parsedResume();
    const create = vi
      .spyOn(api, "createInterview")
      .mockResolvedValue({ ...created, type: "EXPERIENCE" });
    render(<NewInterviewScreen />);

    const experience = await screen.findByRole("radio", { name: /경험 기반 면접/ });
    await waitFor(() => expect(experience).toBeEnabled());
    fireEvent.click(experience);
    next();
    fireEvent.click(await screen.findByRole("radio", { name: /실시간 재고 동기화/ }));
    next();
    fireEvent.click(screen.getByRole("button", { name: "질문 만들기" }));

    await waitFor(() =>
      expect(create).toHaveBeenCalledWith({
        type: "EXPERIENCE",
        count: 3,
        resumeId: 3,
        experienceId: 31,
      }),
    );
  });

  it("인성은 조건 없이 바로 질문 수로", async () => {
    noResume();
    render(<NewInterviewScreen />);
    fireEvent.click(await screen.findByRole("radio", { name: /인성 면접/ }));
    next();
    expect(screen.getByRole("heading", { name: /몇 개/ })).toBeInTheDocument();
  });

  it("질문이 모자라면 원인을 보여준다", async () => {
    noResume();
    vi.spyOn(api, "createInterview").mockRejectedValue(
      new ApiError(409, "NOT_ENOUGH_QUESTIONS", "조건에 맞는 질문이 부족해요. 조건을 넓혀 주세요."),
    );
    render(<NewInterviewScreen />);
    fireEvent.click(await screen.findByRole("radio", { name: /인성 면접/ }));
    next();
    fireEvent.click(screen.getByRole("button", { name: "질문 만들기" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("조건에 맞는 질문이 부족해요.");
  });
});
