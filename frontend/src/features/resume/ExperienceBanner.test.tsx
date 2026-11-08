import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { ExperienceView } from "@/lib/api/client";
import { ExperienceBanner } from "./ExperienceBanner";

const base: ExperienceView = {
  id: 1,
  seq: 0,
  type: "PROJECT",
  title: "실시간 재고 동기화",
  period: "2023.01 - 2023.06",
  summary: "여러 판매 채널의 재고를 Kafka 이벤트로 맞췄습니다.",
  skills: ["Kafka", "Spring Boot"],
};

describe("ExperienceBanner", () => {
  it("종류·기간, 제목, 요약, 기술을 보여준다", () => {
    render(<ExperienceBanner experience={base} />);
    expect(screen.getByText("프로젝트 · 2023.01 - 2023.06")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "실시간 재고 동기화" })).toBeInTheDocument();
    expect(screen.getByText(base.summary)).toBeInTheDocument();
    expect(screen.getByText("Kafka")).toBeInTheDocument();
  });

  it("경력은 '경력'으로 표시하고 기간이 없으면 종류만", () => {
    render(<ExperienceBanner experience={{ ...base, type: "WORK", period: undefined }} />);
    expect(screen.getByText("경력")).toBeInTheDocument();
  });

  it("기술은 4개까지 보여주고 나머지는 +N", () => {
    render(<ExperienceBanner experience={{ ...base, skills: ["A", "B", "C", "D", "E", "F"] }} />);
    expect(screen.getByText("D")).toBeInTheDocument();
    expect(screen.queryByText("E")).not.toBeInTheDocument();
    expect(screen.getByText("+2")).toBeInTheDocument();
  });
});
