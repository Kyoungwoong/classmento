import type { QuestionView } from "@/lib/api/client";

export type Level = QuestionView["level"];
export type JobRole = QuestionView["jobRoles"][number];

/** 화면에 보이는 순서대로 */
export const LEVELS: readonly { value: Level; label: string }[] = [
  { value: "NEW", label: "신입" },
  { value: "JUNIOR", label: "주니어" },
  { value: "MIDDLE", label: "미들" },
  { value: "SENIOR", label: "시니어" },
];

export const JOB_ROLES: readonly { value: JobRole; label: string }[] = [
  { value: "BACKEND", label: "백엔드" },
  { value: "FRONTEND", label: "프론트엔드" },
  { value: "FULLSTACK", label: "풀스택" },
  { value: "MOBILE", label: "모바일" },
  { value: "DATA", label: "데이터" },
  { value: "DEVOPS", label: "DevOps" },
  { value: "COMMON", label: "공통" },
];

export const levelLabel = (v: Level) => LEVELS.find((l) => l.value === v)?.label ?? v;
