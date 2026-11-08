/**
 * 백엔드 호출의 유일한 진입점. 응답 타입은 schema.d.ts(OpenAPI에서 생성, 직접 수정 금지)에서 가져온다.
 * 타입 재생성: 백엔드 실행 후 `pnpm gen:api`
 */
import type { components } from "./schema";

export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export type ApiError = { code: string; message: string };
export type HealthResponse = components["schemas"]["HealthResponse"];

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${API_BASE_URL}${path}`, init);
  if (!res.ok) {
    const body = (await res.json().catch(() => null)) as ApiError | null;
    throw new Error(body?.message ?? "잠시 후 다시 시도해 주세요.");
  }
  return (await res.json()) as T;
}

export const getHealth = () => apiFetch<HealthResponse>("/api/health");
