import { render } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { BottomCTA } from "./BottomCTA";

describe("BottomCTA", () => {
  it("하단 고정 CTA가 있다는 표시를 남긴다(모바일 푸터 여백용)", () => {
    const { container } = render(
      <BottomCTA>
        <button type="button">다음</button>
      </BottomCTA>,
    );
    expect(container.firstChild).toHaveAttribute("data-bottom-cta");
  });
});
