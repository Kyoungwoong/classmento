import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { api, type ExperienceView } from "@/lib/api/client";
import { ExperienceSection } from "./ExperienceSection";

const exp: ExperienceView = {
  id: 1,
  seq: 0,
  type: "PROJECT",
  title: "실시간 재고 동기화",
  summary: "요약",
  skills: [],
};

/** 밖에서 resolve/reject 할 수 있는 promise (중간 상태 확인용) */
function deferred<T>() {
  let resolve!: (v: T) => void;
  let reject!: (e: unknown) => void;
  const promise = new Promise<T>((res, rej) => {
    resolve = res;
    reject = rej;
  });
  return { promise, resolve, reject };
}

afterEach(() => vi.restoreAllMocks());

describe("ExperienceSection", () => {
  it("경험 목록을 배너로 보여준다", async () => {
    vi.spyOn(api, "resumeExperiences").mockResolvedValue([exp]);
    render(<ExperienceSection resumeId={1} version={1} onResync={vi.fn()} />);

    expect(screen.getByText("경험을 불러오고 있어요")).toBeInTheDocument();
    expect(await screen.findByRole("heading", { name: "실시간 재고 동기화" })).toBeInTheDocument();
  });

  it("불러오기 실패 → 다시 시도하면 즉시 불러오는 중으로 바뀌고 목록을 다시 조회한다", async () => {
    const load = vi
      .spyOn(api, "resumeExperiences")
      .mockRejectedValueOnce(new Error("x"))
      .mockReturnValueOnce(deferred<ExperienceView[]>().promise);
    const onResync = vi.fn();
    render(<ExperienceSection resumeId={1} version={1} onResync={onResync} />);

    expect(await screen.findByRole("alert")).toHaveTextContent("경험을 불러오지 못했어요.");
    fireEvent.click(screen.getByRole("button", { name: "다시 시도하기" }));

    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(screen.getByText("경험을 불러오고 있어요")).toBeInTheDocument();
    expect(load).toHaveBeenCalledTimes(2);
    expect(onResync).not.toHaveBeenCalled();
  });

  it("비어 있으면 [경험으로 나누기]가 onResync를 부른다", async () => {
    vi.spyOn(api, "resumeExperiences").mockResolvedValue([]);
    const onResync = vi.fn().mockResolvedValue(undefined);
    render(<ExperienceSection resumeId={1} version={1} onResync={onResync} />);

    fireEvent.click(await screen.findByRole("button", { name: "경험으로 나누기" }));
    await waitFor(() => expect(onResync).toHaveBeenCalledTimes(1));
  });

  it("나누기 실패 → 다시 시도하면 목록 재조회가 아니라 나누기를 다시 하고, 성공하면 새 목록을 보여준다", async () => {
    const load = vi
      .spyOn(api, "resumeExperiences")
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([exp]);
    const retried = deferred<void>();
    const onResync = vi
      .fn()
      .mockRejectedValueOnce(new Error("x"))
      .mockReturnValueOnce(retried.promise);
    render(<ExperienceSection resumeId={1} version={1} onResync={onResync} />);

    fireEvent.click(await screen.findByRole("button", { name: "경험으로 나누기" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("경험으로 나누지 못했어요");

    fireEvent.click(screen.getByRole("button", { name: "다시 시도하기" }));
    await waitFor(() => expect(onResync).toHaveBeenCalledTimes(2));
    // 나누는 중: 에러는 사라지고 목록 재조회는 아직 하지 않는다
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(load).toHaveBeenCalledTimes(1);

    await act(async () => retried.resolve());
    expect(await screen.findByRole("heading", { name: "실시간 재고 동기화" })).toBeInTheDocument();
    expect(load).toHaveBeenCalledTimes(2);
  });

  it("나누기에 성공하면 부모가 version을 바꾸지 않아도 스스로 목록을 다시 불러온다", async () => {
    const load = vi
      .spyOn(api, "resumeExperiences")
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([exp]);
    const onResync = vi.fn().mockResolvedValue(undefined);
    render(<ExperienceSection resumeId={1} version={1} onResync={onResync} />);

    fireEvent.click(await screen.findByRole("button", { name: "경험으로 나누기" }));

    expect(await screen.findByRole("heading", { name: "실시간 재고 동기화" })).toBeInTheDocument();
    expect(load).toHaveBeenCalledTimes(2);
  });

  it("나누기 성공 직후 새 목록이 오기 전에는 '찾지 못했어요'를 보이지 않고 불러오는 중으로 둔다", async () => {
    const reloaded = deferred<ExperienceView[]>();
    vi.spyOn(api, "resumeExperiences")
      .mockResolvedValueOnce([])
      .mockReturnValueOnce(reloaded.promise);
    const onResync = vi.fn().mockResolvedValue(undefined);
    render(<ExperienceSection resumeId={1} version={1} onResync={onResync} />);

    fireEvent.click(await screen.findByRole("button", { name: "경험으로 나누기" }));
    await waitFor(() => expect(onResync).toHaveBeenCalled());
    await act(async () => {});

    // 재조회 결과가 오기 전
    expect(screen.queryByText("이력서에서 경력·프로젝트를 찾지 못했어요.")).not.toBeInTheDocument();
    expect(screen.getByText("경험을 불러오고 있어요")).toBeInTheDocument();

    // 재조회 결과가 정말 0건이면 그때 문구를 보여주고 버튼은 숨긴다
    await act(async () => reloaded.resolve([]));
    expect(screen.getByText("이력서에서 경력·프로젝트를 찾지 못했어요.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "경험으로 나누기" })).not.toBeInTheDocument();
  });
});
