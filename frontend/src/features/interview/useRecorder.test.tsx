import { act, renderHook } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MAX_SECONDS, useRecorder } from "./useRecorder";

/** 브라우저 MediaRecorder 흉내: stop하면 쌓인 조각을 내보낸다 */
class FakeRecorder {
  static isTypeSupported = (t: string) => t.startsWith("audio/webm");
  state = "inactive";
  mimeType: string;
  ondataavailable: ((e: { data: Blob }) => void) | null = null;
  onstop: (() => void) | null = null;
  constructor(_stream: unknown, opts?: { mimeType?: string }) {
    this.mimeType = opts?.mimeType ?? "audio/webm";
  }
  start() {
    this.state = "recording";
  }
  stop() {
    this.state = "inactive";
    this.ondataavailable?.({ data: new Blob(["voice"], { type: this.mimeType }) });
    this.onstop?.();
  }
}

const track = { stop: vi.fn() };

beforeEach(() => {
  vi.useFakeTimers();
  vi.stubGlobal("MediaRecorder", FakeRecorder);
  vi.stubGlobal("URL", { ...URL, createObjectURL: () => "blob:x", revokeObjectURL: vi.fn() });
  Object.defineProperty(navigator, "mediaDevices", {
    configurable: true,
    value: { getUserMedia: vi.fn().mockResolvedValue({ getTracks: () => [track] }) },
  });
});

afterEach(() => {
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

describe("useRecorder", () => {
  it("녹음 → 멈추면 녹음 파일과 다시 듣기 주소가 생기고 마이크를 끈다", async () => {
    const { result } = renderHook(() => useRecorder());
    await act(async () => {
      await result.current.start();
    });
    expect(result.current.state).toBe("recording");

    act(() => vi.advanceTimersByTime(3000));
    expect(result.current.elapsed).toBe(3);

    act(() => result.current.stop());
    expect(result.current.state).toBe("recorded");
    expect(result.current.blob?.type).toBe("audio/webm;codecs=opus");
    expect(result.current.url).toBe("blob:x");
    expect(track.stop).toHaveBeenCalled();
  });

  it("3분이 되면 저절로 멈춘다", async () => {
    const { result } = renderHook(() => useRecorder());
    await act(async () => {
      await result.current.start();
    });
    act(() => vi.advanceTimersByTime(MAX_SECONDS * 1000));
    expect(result.current.state).toBe("recorded");
  });

  it("마이크를 허용하지 않으면 거부 상태", async () => {
    vi.mocked(navigator.mediaDevices.getUserMedia).mockRejectedValueOnce(
      new DOMException("no", "NotAllowedError"),
    );
    const { result } = renderHook(() => useRecorder());
    await act(async () => {
      await result.current.start();
    });
    expect(result.current.state).toBe("denied");
  });

  it("녹음기를 만들지 못하면 녹음할 수 없음 상태로 두고 마이크를 끈다", async () => {
    vi.stubGlobal(
      "MediaRecorder",
      Object.assign(
        function () {
          throw new Error("NotSupportedError");
        },
        { isTypeSupported: () => true },
      ),
    );
    track.stop.mockClear();
    const { result } = renderHook(() => useRecorder());
    await act(async () => {
      await result.current.start();
    });
    expect(result.current.state).toBe("unsupported");
    expect(track.stop).toHaveBeenCalled();
  });

  it("다시 녹음하면 처음 상태로", async () => {
    const { result } = renderHook(() => useRecorder());
    await act(async () => {
      await result.current.start();
    });
    act(() => result.current.stop());
    act(() => result.current.reset());
    expect(result.current.state).toBe("idle");
    expect(result.current.blob).toBeNull();
  });
  it("시작 중에 또 누르면 마이크를 한 번만 연다", async () => {
    const { result } = renderHook(() => useRecorder());
    await act(async () => {
      await Promise.all([result.current.start(), result.current.start()]);
    });
    expect(navigator.mediaDevices.getUserMedia).toHaveBeenCalledTimes(1);
    expect(result.current.state).toBe("recording");
  });
  it("마이크를 여는 중에 화면을 떠나면 열린 마이크를 바로 끈다", async () => {
    let resolve!: (s: unknown) => void;
    const late = { stop: vi.fn() };
    vi.mocked(navigator.mediaDevices.getUserMedia).mockReturnValueOnce(
      new Promise((r) => (resolve = r)) as Promise<MediaStream>,
    );
    const { result, unmount } = renderHook(() => useRecorder());
    let pending!: Promise<void>;
    act(() => {
      pending = result.current.start();
    });
    unmount();
    await act(async () => {
      resolve({ getTracks: () => [late] });
      await pending;
    });
    expect(late.stop).toHaveBeenCalled();
  });
});
