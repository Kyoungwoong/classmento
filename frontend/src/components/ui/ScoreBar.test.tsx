import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ScoreBar } from "./ScoreBar";

describe("ScoreBar", () => {
  it("정수 점수는 그대로, 평균은 소수 한 자리로 보여준다", () => {
    render(
      <>
        <ScoreBar label="정확성" score={4} />
        <ScoreBar label="전달력" score={3.25} />
      </>,
    );
    expect(screen.getByRole("progressbar", { name: "정확성 4점" })).toBeInTheDocument();
    expect(screen.getByRole("progressbar", { name: "전달력 3.3점" })).toBeInTheDocument();
    expect(screen.getByText("3.3")).toBeInTheDocument();
  });
});
