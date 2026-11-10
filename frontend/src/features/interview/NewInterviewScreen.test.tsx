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
  maxFollowUps: 0,
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
        followUps: 1,
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
        followUps: 1,
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

  it("기술 면접에서 이력서로 좁히면 어느 이력서인지 보여 주고 resumeId를 보낸다", async () => {
    parsedResume();
    const create = vi.spyOn(api, "createInterview").mockResolvedValue(created);
    render(<NewInterviewScreen />);
    fireEvent.click(await screen.findByRole("radio", { name: /기술 면접/ }));
    next();
    fireEvent.click(screen.getByRole("button", { name: "백엔드" }));
    fireEvent.click(screen.getByRole("radio", { name: "미들" }));
    fireEvent.click(screen.getByRole("switch", { name: /me\.pdf 기술로 좁히기/ }));
    next();
    fireEvent.click(screen.getByRole("button", { name: "질문 만들기" }));
    await waitFor(() =>
      expect(create).toHaveBeenCalledWith({
        type: "TECH",
        count: 3,
        level: "MIDDLE",
        jobRole: "BACKEND",
        narrowByResume: true,
        resumeId: 3,
        followUps: 1,
      }),
    );
  });

  it("혼합은 직무·연차만 보낸다", async () => {
    noResume();
    const create = vi.spyOn(api, "createInterview").mockResolvedValue(created);
    render(<NewInterviewScreen />);
    fireEvent.click(await screen.findByRole("radio", { name: /기술 \+ 인성/ }));
    next();
    fireEvent.click(screen.getByRole("button", { name: "프론트엔드" }));
    fireEvent.click(screen.getByRole("radio", { name: "신입" }));
    next();
    fireEvent.click(screen.getByRole("button", { name: "질문 만들기" }));
    await waitFor(() =>
      expect(create).toHaveBeenCalledWith({
        type: "MIXED",
        count: 3,
        level: "NEW",
        jobRole: "FRONTEND",
        followUps: 1,
      }),
    );
  });

  it("뒤로 가기는 이전 단계로", async () => {
    noResume();
    render(<NewInterviewScreen />);
    fireEvent.click(await screen.findByRole("radio", { name: /인성 면접/ }));
    next();
    fireEvent.click(screen.getByRole("button", { name: "뒤로 가기" }));
    expect(screen.getByRole("heading", { name: "어떤 면접을 볼까요?" })).toBeInTheDocument();
    expect(router.back).not.toHaveBeenCalled();
  });

  it("경험을 못 불러오면 다시 불러올 수 있다", async () => {
    vi.spyOn(api, "resumes").mockResolvedValue([
      { id: 3, filename: "me.pdf", status: "PARSED", createdAt: "2026-11-09T00:00:00Z" },
    ]);
    const load = vi
      .spyOn(api, "resumeExperiences")
      .mockRejectedValueOnce(new Error("x"))
      .mockResolvedValue([exp]);
    render(<NewInterviewScreen />);
    const experience = await screen.findByRole("radio", { name: /경험 기반 면접/ });
    await waitFor(() => expect(experience).toBeEnabled());
    fireEvent.click(experience);
    next();
    expect(await screen.findByRole("alert")).toHaveTextContent("경험을 불러오지 못했어요");
    fireEvent.click(screen.getByRole("button", { name: "다시 불러오기" }));
    expect(await screen.findByRole("radio", { name: /실시간 재고 동기화/ })).toBeInTheDocument();
    expect(load).toHaveBeenCalledTimes(2);
  });

  it("올렸지만 정리 중인 이력서면 다시 올리라고 하지 않는다", async () => {
    vi.spyOn(api, "resumes").mockResolvedValue([
      { id: 4, filename: "new.pdf", status: "EXTRACTED", createdAt: "2026-11-09T00:00:00Z" },
    ]);
    render(<NewInterviewScreen />);
    expect(await screen.findByRole("radio", { name: /경험 기반 면접/ })).toHaveTextContent(
      "이력서를 정리하고 있어요",
    );
    expect(screen.queryByRole("link", { name: "이력서 올리러 가기" })).not.toBeInTheDocument();
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

describe("NewInterviewScreen 단계 안내", () => {
  it("단계 목록이 지금 단계를 알려 주고, 인성은 조건 단계를 건너뛴다", async () => {
    noResume();
    render(<NewInterviewScreen />);
    const steps = screen.getByRole("list", { name: "면접 만들기 단계" });
    expect(steps).toHaveTextContent("면접 종류");
    expect(steps).toHaveTextContent("조건");
    expect(steps.querySelector("[aria-current='step']")).toHaveTextContent("면접 종류");

    fireEvent.click(await screen.findByRole("radio", { name: /인성 면접/ }));
    next();
    expect(steps).not.toHaveTextContent("조건");
    expect(steps.querySelector("[aria-current='step']")).toHaveTextContent("질문 수");
    // 지나온 단계는 고른 값과 "완료"
    expect(steps.querySelector("li")).toHaveTextContent("인성 면접 · 완료");
  });
});

describe("NewInterviewScreen 꼬리질문 옵션", () => {
  it("기술 면접은 꼬리질문을 고를 수 있고(기본 한 번) 고른 값을 보낸다", async () => {
    noResume();
    const create = vi.spyOn(api, "createInterview").mockResolvedValue(created);
    render(<NewInterviewScreen />);
    fireEvent.click(await screen.findByRole("radio", { name: /기술 면접/ }));
    next();
    fireEvent.click(screen.getByRole("button", { name: "백엔드" }));
    fireEvent.click(screen.getByRole("radio", { name: "주니어" }));
    next();

    const group = screen.getByRole("radiogroup", { name: "꼬리질문" });
    expect(group).toBeInTheDocument();
    expect(screen.getByRole("radio", { name: "한 번" })).toBeChecked();
    fireEvent.click(screen.getByRole("radio", { name: "두 번" }));
    fireEvent.click(screen.getByRole("button", { name: "질문 만들기" }));

    await waitFor(() =>
      expect(create).toHaveBeenCalledWith(expect.objectContaining({ followUps: 2 })),
    );
  });

  it("인성 면접은 꼬리질문 옵션이 없고 0을 보낸다", async () => {
    noResume();
    const create = vi.spyOn(api, "createInterview").mockResolvedValue(created);
    render(<NewInterviewScreen />);
    fireEvent.click(await screen.findByRole("radio", { name: /인성 면접/ }));
    next();
    expect(screen.queryByRole("radiogroup", { name: "꼬리질문" })).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "질문 만들기" }));
    await waitFor(() =>
      expect(create).toHaveBeenCalledWith({ type: "BEHAVIORAL", count: 3, followUps: 0 }),
    );
  });
});
