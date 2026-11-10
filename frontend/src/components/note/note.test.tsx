import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import {
  CheckList,
  GridPaper,
  HandNote,
  Highlight,
  NoteSheet,
  Scribble,
  Stamp,
  StickyNote,
} from "./index";

describe("노트 부품", () => {
  it("모눈·밑줄은 장식이라 읽히지 않는다", () => {
    const { container } = render(
      <>
        <GridPaper />
        <Scribble />
      </>,
    );
    container.querySelectorAll("svg").forEach((svg) => {
      expect(svg).toHaveAttribute("aria-hidden", "true");
    });
  });

  it("형광펜은 글자를 그대로 읽히게 하고 칠한 면만 숨긴다", () => {
    render(
      <h1>
        면접은 <Highlight>말로</Highlight> 해요
      </h1>,
    );
    expect(screen.getByRole("heading", { name: "면접은 말로 해요" })).toBeInTheDocument();
  });

  it("노트 한 장은 내용을 담고 스프링 구멍은 숨긴다", () => {
    render(
      <NoteSheet aria-label="노트">
        <p>질문</p>
      </NoteSheet>,
    );
    const sheet = screen.getByLabelText("노트");
    expect(sheet).toHaveTextContent("질문");
    expect(sheet.querySelector("[data-holes]")).toHaveAttribute("aria-hidden", "true");
  });

  it("도장은 점수를 그림 이름으로 읽힌다", () => {
    render(<Stamp score={4.2} />);
    const stamp = screen.getByRole("img", { name: "종합 4.2점, 5점 만점" });
    expect(stamp).toHaveTextContent("4.2");
  });

  it("포스트잇은 제목과 내용을 보여 준다", () => {
    render(<StickyNote title="이렇게 답해 보세요">결론부터</StickyNote>);
    expect(screen.getByRole("heading", { name: "이렇게 답해 보세요" })).toBeInTheDocument();
    expect(screen.getByText("결론부터")).toBeInTheDocument();
  });

  it("체크리스트는 끝난 항목을 '완료'로 읽힌다", () => {
    render(
      <CheckList
        label="준비"
        items={[
          { title: "이력서 올리기", description: "PDF", done: true },
          { title: "면접 고르기", description: "네 가지" },
        ]}
      />,
    );
    const list = screen.getByRole("list", { name: "준비" });
    const items = list.querySelectorAll("li");
    expect(items[0]).toHaveTextContent("완료");
    expect(items[1]).not.toHaveTextContent("완료");
  });

  it("손글씨 메모는 손글씨 폰트를 쓴다", () => {
    render(<HandNote>3분이면 한 문제</HandNote>);
    expect(screen.getByText("3분이면 한 문제")).toHaveClass("font-hand");
  });
});

describe("NoteSheet active", () => {
  it("active면 놓는 중처럼 연한 연두 면과 진한 연두 테두리", () => {
    render(
      <NoteSheet active aria-label="노트">
        <p>파일</p>
      </NoteSheet>,
    );
    const sheet = screen.getByLabelText("노트");
    expect(sheet).toHaveClass("bg-bg-tint", "border-accent");
    expect(sheet).not.toHaveClass("bg-paper");
  });
});
