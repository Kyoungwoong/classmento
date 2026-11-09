/**
 * 백엔드 호출의 유일한 진입점. 응답 타입은 schema.d.ts(OpenAPI에서 생성, 직접 수정 금지)에서 가져온다.
 * 타입 재생성: 백엔드 실행 후 `pnpm gen:api`
 *
 * - 브라우저는 같은 출처의 /api 를 부르고 next.config.ts rewrites가 백엔드로 넘긴다.
 * - 로그인은 HttpOnly 세션 쿠키. 변경 요청(POST/PUT/DELETE)에는 CSRF 토큰 헤더를 자동으로 붙인다.
 */
import type { components, operations } from "./schema";

type Schemas = components["schemas"];

export type HealthResponse = Schemas["HealthResponse"];
export type UserView = Schemas["UserView"];
export type MeResponse = Schemas["MeResponse"];
export type SocialProvider = Schemas["SocialProvider"];
export type ResumeDetail = Schemas["ResumeDetail"];
export type ResumeSummary = Schemas["ResumeSummary"];
export type ParsedResume = Schemas["ParsedResume"];
export type ExperienceView = Schemas["ExperienceView"];
export type CategoryTreeNode = Schemas["CategoryTreeNode"];
export type CategoryView = Schemas["CategoryView"];
export type SubcategoryView = Schemas["SubcategoryView"];
export type CreateCategoryRequest = Schemas["CreateCategoryRequest"];
export type UpdateCategoryRequest = Schemas["UpdateCategoryRequest"];
export type TagView = Schemas["TagView"];
export type TagResult = Schemas["TagResult"];
export type QuestionView = Schemas["QuestionView"];
export type QuestionPage = Schemas["QuestionPage"];
export type QuestionRequest = Schemas["QuestionRequest"];
export type ImportResult = Schemas["ImportResult"];
export type QuestionSearchParams = NonNullable<
  operations["searchAdminQuestions"]["parameters"]["query"]
>;
export type AdminUserView = Schemas["AdminUserView"];
export type SignupRequest = Schemas["SignupRequest"];
export type LoginRequest = Schemas["LoginRequest"];

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message);
  }
}

const CSRF_COOKIE = "XSRF-TOKEN";
const CSRF_HEADER = "X-XSRF-TOKEN";

function readCookie(name: string): string | undefined {
  if (typeof document === "undefined") return undefined;
  return document.cookie
    .split("; ")
    .find((c) => c.startsWith(`${name}=`))
    ?.slice(name.length + 1);
}

async function csrfToken(): Promise<string> {
  const existing = readCookie(CSRF_COOKIE);
  if (existing) return decodeURIComponent(existing);
  const res = await fetch("/api/auth/csrf", { credentials: "same-origin" });
  const body = (await res.json()) as { token: string };
  return readCookie(CSRF_COOKIE) ? decodeURIComponent(readCookie(CSRF_COOKIE)!) : body.token;
}

export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(init.headers);
  if (method !== "GET" && method !== "HEAD") {
    headers.set(CSRF_HEADER, await csrfToken());
  }
  if (init.body && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const res = await fetch(path, { ...init, headers, credentials: "same-origin" });
  if (!res.ok) {
    const body = (await res.json().catch(() => null)) as { code?: string; message?: string } | null;
    throw new ApiError(
      res.status,
      body?.code ?? (res.status === 401 ? "UNAUTHORIZED" : "ERROR"),
      body?.message ?? defaultMessage(res.status),
    );
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

function defaultMessage(status: number) {
  if (status === 401) return "로그인이 필요해요.";
  if (status === 403) return "권한이 없어요.";
  if (status === 404) return "찾을 수 없어요.";
  return "잠시 후 다시 시도해 주세요.";
}

const json = (body: unknown) => JSON.stringify(body);

/** 값이 있는 항목만 ?a=1&b=2 로. 없으면 빈 문자열. */
function queryString(params: Record<string, string | number | boolean | undefined>) {
  const q = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== "") q.set(k, String(v));
  }
  const s = q.toString();
  return s ? `?${s}` : "";
}

export const api = {
  health: () => apiFetch<HealthResponse>("/api/health"),

  // 인증
  me: () => apiFetch<MeResponse>("/api/auth/me"),
  providers: () => apiFetch<{ providers: SocialProvider[] }>("/api/auth/providers"),
  signup: (body: SignupRequest) =>
    apiFetch<UserView>("/api/auth/signup", { method: "POST", body: json(body) }),
  login: (body: LoginRequest) =>
    apiFetch<UserView>("/api/auth/login", { method: "POST", body: json(body) }),
  logout: () => apiFetch<void>("/api/auth/logout", { method: "POST" }),

  // 이력서
  uploadResume: (file: File) => {
    const form = new FormData();
    form.append("file", file);
    return apiFetch<ResumeDetail>("/api/resumes", { method: "POST", body: form });
  },
  resumes: () => apiFetch<ResumeSummary[]>("/api/resumes"),
  resume: (id: number) => apiFetch<ResumeDetail>(`/api/resumes/${id}`),
  resumeExperiences: (id: number) => apiFetch<ExperienceView[]>(`/api/resumes/${id}/experiences`),
  updateParsed: (id: number, parsed: ParsedResume) =>
    apiFetch<ResumeDetail>(`/api/resumes/${id}/parsed`, { method: "PUT", body: json(parsed) }),

  // 관리자
  adminUsers: () => apiFetch<AdminUserView[]>("/api/admin/users"),

  // 관리자: 질문 은행 카테고리·태그
  adminCategories: () => apiFetch<CategoryTreeNode[]>("/api/admin/categories"),
  createCategory: (body: CreateCategoryRequest) =>
    apiFetch<CategoryView>("/api/admin/categories", { method: "POST", body: json(body) }),
  updateCategory: (id: number, body: UpdateCategoryRequest) =>
    apiFetch<CategoryView>(`/api/admin/categories/${id}`, { method: "PATCH", body: json(body) }),
  suggestTags: (query: string) =>
    apiFetch<TagView[]>(`/api/admin/tags?query=${encodeURIComponent(query)}`),
  createTag: (name: string) =>
    apiFetch<TagResult>("/api/admin/tags", { method: "POST", body: json({ name }) }),

  // 관리자: 질문 은행 질문
  adminQuestions: (params: QuestionSearchParams = {}) =>
    apiFetch<QuestionPage>(`/api/admin/questions${queryString(params)}`),
  adminQuestion: (id: number) => apiFetch<QuestionView>(`/api/admin/questions/${id}`),
  createQuestion: (body: QuestionRequest) =>
    apiFetch<QuestionView>("/api/admin/questions", { method: "POST", body: json(body) }),
  updateQuestion: (id: number, body: QuestionRequest) =>
    apiFetch<QuestionView>(`/api/admin/questions/${id}`, { method: "PUT", body: json(body) }),
  importQuestions: (file: File) => {
    const form = new FormData();
    form.append("file", file);
    return apiFetch<ImportResult>("/api/admin/questions/import", { method: "POST", body: form });
  },
  setQuestionActive: (id: number, active: boolean) =>
    apiFetch<QuestionView>(`/api/admin/questions/${id}/active`, {
      method: "PATCH",
      body: json({ active }),
    }),
};
