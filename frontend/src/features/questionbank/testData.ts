import type { CategoryTreeNode, QuestionView } from "@/lib/api/client";

/** 테스트용 카테고리 트리: CS(네트워크·보안), 인성(협업, 숨김 소분류 갈등) */
export const tree: CategoryTreeNode[] = [
  {
    id: 1,
    name: "CS",
    sortOrder: 0,
    active: true,
    children: [
      { id: 11, parentId: 1, name: "네트워크", sortOrder: 0, active: true, questionCount: 2 },
      { id: 12, parentId: 1, name: "보안", sortOrder: 1, active: true, questionCount: 0 },
    ],
  },
  {
    id: 2,
    name: "인성",
    sortOrder: 1,
    active: true,
    children: [
      { id: 21, parentId: 2, name: "협업", sortOrder: 0, active: true, questionCount: 1 },
      { id: 22, parentId: 2, name: "갈등", sortOrder: 1, active: false, questionCount: 0 },
    ],
  },
];

export function question(overrides: Partial<QuestionView> = {}): QuestionView {
  return {
    id: 100,
    topCategoryId: 1,
    topCategoryName: "CS",
    categoryId: 11,
    categoryName: "네트워크",
    text: "TCP와 UDP의 차이를 설명해 주세요.",
    level: "NEW",
    jobRoles: ["COMMON"],
    tags: [],
    active: true,
    createdAt: "2026-11-09T00:00:00Z",
    ...overrides,
  };
}
