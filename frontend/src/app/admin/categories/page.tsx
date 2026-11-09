import type { Metadata } from "next";
import { CategoriesScreen } from "@/features/questionbank/CategoriesScreen";

export const metadata: Metadata = { title: "카테고리 · 관리자 · 클래스멘토" };

export default function AdminCategoriesPage() {
  return <CategoriesScreen />;
}
