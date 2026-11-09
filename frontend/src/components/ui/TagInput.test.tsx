import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { useState } from "react";
import { describe, expect, it, vi } from "vitest";
import { TagInput, type TagItem } from "./TagInput";

function Harness(props: {
  suggest?: (q: string) => Promise<TagItem[]>;
  create?: (name: string) => Promise<TagItem>;
  initial?: TagItem[];
}) {
  const [tags, setTags] = useState<TagItem[]>(props.initial ?? []);
  return (
    <TagInput
      label="태그"
      value={tags}
      onChange={setTags}
      suggest={props.suggest ?? (async () => [])}
      create={props.create ?? (async (name) => ({ id: 99, name }))}
    />
  );
}

describe("TagInput", () => {
  it("입력하면 자동완성을 보여주고, 고르면 칩으로 붙는다", async () => {
    const suggest = vi.fn().mockResolvedValue([{ id: 1, name: "JPA" }]);
    render(<Harness suggest={suggest} />);
    fireEvent.change(screen.getByLabelText("태그"), { target: { value: "jp" } });
    fireEvent.click(await screen.findByRole("option", { name: "JPA" }));
    expect(screen.getByRole("button", { name: "JPA 지우기" })).toBeInTheDocument();
    expect(screen.getByLabelText("태그")).toHaveValue("");
    await waitFor(() => expect(suggest).toHaveBeenCalledWith("jp"));
  });

  it("Enter는 새 태그를 만들어 붙인다", async () => {
    const create = vi.fn().mockResolvedValue({ id: 7, name: "Redis" });
    render(<Harness create={create} />);
    const input = screen.getByLabelText("태그");
    fireEvent.change(input, { target: { value: "Redis" } });
    fireEvent.keyDown(input, { key: "Enter" });
    expect(await screen.findByRole("button", { name: "Redis 지우기" })).toBeInTheDocument();
    expect(create).toHaveBeenCalledWith("Redis");
  });

  it("쉼표도 확정 키다", async () => {
    const create = vi.fn().mockResolvedValue({ id: 3, name: "트랜잭션" });
    render(<Harness create={create} />);
    fireEvent.change(screen.getByLabelText("태그"), { target: { value: "트랜잭션," } });
    expect(await screen.findByRole("button", { name: "트랜잭션 지우기" })).toBeInTheDocument();
    expect(screen.getByLabelText("태그")).toHaveValue("");
  });

  it("이미 붙은 태그(대소문자 무시)는 다시 만들지 않는다", async () => {
    const create = vi.fn();
    render(<Harness create={create} initial={[{ id: 1, name: "JPA" }]} />);
    fireEvent.change(screen.getByLabelText("태그"), { target: { value: "jpa," } });
    await waitFor(() => expect(screen.getByLabelText("태그")).toHaveValue(""));
    expect(create).not.toHaveBeenCalled();
    expect(screen.getAllByRole("button", { name: "JPA 지우기" })).toHaveLength(1);
  });

  it("한글 조합 중 Enter는 태그를 만들지 않는다", () => {
    const create = vi.fn();
    render(<Harness create={create} />);
    const input = screen.getByLabelText("태그");
    fireEvent.change(input, { target: { value: "네트" } });
    fireEvent.keyDown(input, { key: "Enter", isComposing: true });
    expect(create).not.toHaveBeenCalled();
    expect(input).toHaveValue("네트");
  });

  it("칩의 지우기를 누르면 빠진다", () => {
    render(<Harness initial={[{ id: 1, name: "JPA" }]} />);
    fireEvent.click(screen.getByRole("button", { name: "JPA 지우기" }));
    expect(screen.queryByRole("button", { name: "JPA 지우기" })).not.toBeInTheDocument();
  });

  it("만들기가 실패하면 칸 아래에 에러를 보여준다", async () => {
    const create = vi.fn().mockRejectedValue(new Error("x"));
    render(<Harness create={create} />);
    const input = screen.getByLabelText("태그");
    fireEvent.change(input, { target: { value: "a,b" } });
    fireEvent.keyDown(input, { key: "Enter" });
    expect(
      await screen.findByText("태그를 만들지 못했어요. 다시 시도해 주세요."),
    ).toBeInTheDocument();
  });
});
