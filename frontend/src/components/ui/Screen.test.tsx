import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { BottomCTA } from "./BottomCTA";
import { Screen } from "./Screen";

describe("Screen", () => {
  it("aside를 주면 2단: 왼쪽 aside, 오른쪽 본문 끝에 주요 행동", () => {
    render(
      <Screen
        aside={<p>왼쪽 안내</p>}
        cta={
          <BottomCTA>
            <button type="button">다음</button>
          </BottomCTA>
        }
      >
        <p>오른쪽 본문</p>
      </Screen>,
    );
    const main = screen.getByText("오른쪽 본문").parentElement!;
    // 주요 행동은 본문 열 안(데스크톱에서 패널 바로 아래)
    expect(main).toContainElement(screen.getByRole("button", { name: "다음" }));
    expect(main).not.toContainElement(screen.getByText("왼쪽 안내"));
  });

  it("aside가 없으면 한 열", () => {
    const { container } = render(
      <Screen>
        <p>본문</p>
      </Screen>,
    );
    expect(container.querySelector("[class*='md:grid-cols-12']")).toBeNull();
  });
});
