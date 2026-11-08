"use client";

import { useEffect, useState } from "react";
import { api, ApiError, type ResumeDetail } from "@/lib/api/client";

const POLL_MS = 1500;
const DONE = new Set(["PARSED", "FAILED"]);

/** 이력서 상세를 불러오고, 처리 중(UPLOADED/EXTRACTED)이면 끝날 때까지 1.5초마다 다시 묻는다. */
export function useResume(id: number) {
  const [resume, setResume] = useState<ResumeDetail | null>(null);
  const [error, setError] = useState<ApiError | null>(null);

  useEffect(() => {
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout>;

    async function load() {
      try {
        const r = await api.resume(id);
        if (cancelled) return;
        setResume(r);
        if (!DONE.has(r.status)) timer = setTimeout(load, POLL_MS);
      } catch (err) {
        if (!cancelled)
          setError(
            err instanceof ApiError ? err : new ApiError(0, "ERROR", "잠시 후 다시 시도해 주세요."),
          );
      }
    }
    load();
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [id]);

  return { resume, setResume, error };
}
