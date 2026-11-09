"use client";

import { useCallback, useEffect, useRef, useState } from "react";

/** 답변 하나의 최대 길이(초). 넘으면 저절로 멈춘다(서버 10MB 제한 안쪽). */
export const MAX_SECONDS = 180;

export type RecorderState =
  "idle" | "requesting" | "recording" | "recorded" | "denied" | "unsupported";

/** 브라우저가 녹음할 수 있는 형식 중 서버가 받는 것(먼저 나온 것 우선) */
const PREFERRED = ["audio/webm;codecs=opus", "audio/webm", "audio/ogg;codecs=opus", "audio/mp4"];

function pickMimeType(): string | undefined {
  if (typeof MediaRecorder === "undefined" || !MediaRecorder.isTypeSupported) return undefined;
  return PREFERRED.find((t) => MediaRecorder.isTypeSupported(t));
}

/**
 * 마이크 녹음(MediaRecorder). 상태: 대기 → 녹음 중(경과 초) → 녹음됨(blob·다시 듣기 주소) / 거부 / 지원 안 함.
 * 멈추면 마이크를 끈다. 3분이 되면 저절로 멈춘다.
 */
export function useRecorder() {
  const [state, setState] = useState<RecorderState>("idle");
  const [elapsed, setElapsed] = useState(0);
  const [blob, setBlob] = useState<Blob | null>(null);
  const [url, setUrl] = useState<string | null>(null);
  const recorder = useRef<MediaRecorder | null>(null);
  const stream = useRef<MediaStream | null>(null);
  const timer = useRef<ReturnType<typeof setInterval> | null>(null);
  // 마이크를 여는 중에 또 시작하면 스트림이 둘 생겨 하나가 꺼지지 않는다
  const starting = useRef(false);
  const seconds = useRef(0);

  const clearTimer = () => {
    if (timer.current) clearInterval(timer.current);
    timer.current = null;
  };

  const stop = useCallback(() => {
    clearTimer();
    if (recorder.current && recorder.current.state !== "inactive") recorder.current.stop();
  }, []);

  const start = useCallback(async () => {
    if (starting.current || recorder.current?.state === "recording") return;
    if (typeof navigator === "undefined" || !navigator.mediaDevices?.getUserMedia) {
      setState("unsupported");
      return;
    }
    starting.current = true;
    setState("requesting");
    let media: MediaStream;
    try {
      media = await navigator.mediaDevices.getUserMedia({ audio: true });
    } catch {
      starting.current = false;
      setState("denied");
      return;
    }
    const mimeType = pickMimeType();
    let rec: MediaRecorder;
    try {
      rec = new MediaRecorder(media, mimeType ? { mimeType } : undefined);
    } catch {
      // 형식을 지원하지 않거나 마이크 스트림이 끊긴 경우
      media.getTracks().forEach((t) => t.stop());
      starting.current = false;
      setState("unsupported");
      return;
    }
    const chunks: Blob[] = [];
    rec.ondataavailable = (e) => {
      if (e.data.size > 0) chunks.push(e.data);
    };
    rec.onstop = () => {
      media.getTracks().forEach((t) => t.stop());
      const out = new Blob(chunks, { type: mimeType ?? rec.mimeType ?? "audio/webm" });
      setBlob(out);
      setUrl(URL.createObjectURL(out));
      setState("recorded");
    };
    // 남아 있던 스트림이 있으면 끈다
    if (stream.current && stream.current !== media) {
      stream.current.getTracks().forEach((t) => t.stop());
    }
    recorder.current = rec;
    stream.current = media;
    setBlob(null);
    seconds.current = 0;
    setElapsed(0);
    rec.start();
    starting.current = false;
    setState("recording");
    timer.current = setInterval(() => {
      seconds.current += 1;
      setElapsed(seconds.current);
      if (seconds.current >= MAX_SECONDS) stop();
    }, 1000);
  }, [stop]);

  const reset = useCallback(() => {
    clearTimer();
    setBlob(null);
    seconds.current = 0;
    setElapsed(0);
    setState("idle");
  }, []);

  // 다시 듣기 주소는 바뀌거나 화면을 떠날 때 해제
  useEffect(() => {
    return () => {
      if (url) URL.revokeObjectURL(url);
    };
  }, [url]);

  // 화면을 떠나면 녹음·마이크를 멈춘다
  useEffect(
    () => () => {
      clearTimer();
      stream.current?.getTracks().forEach((t) => t.stop());
    },
    [],
  );

  return { state, elapsed, blob, url: state === "recorded" ? url : null, start, stop, reset };
}
