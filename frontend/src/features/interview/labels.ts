import type { InterviewSummary } from "@/lib/api/client";

/** 평가 루브릭 기준 이름(docs/ai/interview-and-rubric.md) */
export const CRITERION_LABEL: Record<string, string> = {
  accuracy: "정확성",
  depth: "깊이·근거",
  problem_solving: "문제 해결",
  communication: "전달력",
  specificity: "구체성",
  ownership: "주도성",
  collaboration: "협업",
  reflection: "배운 점",
};

export const criterionLabel = (c: string) => CRITERION_LABEL[c] ?? c;

export const TYPE_LABEL: Record<InterviewSummary["type"], string> = {
  EXPERIENCE: "경험 기반 면접",
  TECH: "기술 면접",
  BEHAVIORAL: "인성 면접",
  MIXED: "기술 + 인성",
};
