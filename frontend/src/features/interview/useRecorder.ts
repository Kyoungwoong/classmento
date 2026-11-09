"use client";

export const MAX_SECONDS = 180;

export type RecorderState = "idle" | "recording" | "recorded" | "denied" | "unsupported";

/** 마이크 녹음(빈 구현) */
export function useRecorder() {
  return {
    state: "idle" as RecorderState,
    elapsed: 0,
    blob: null as Blob | null,
    url: null as string | null,
    start: async () => {},
    stop: () => {},
    reset: () => {},
  };
}
