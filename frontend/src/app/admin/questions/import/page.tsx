import type { Metadata } from "next";
import { QuestionImportScreen } from "@/features/questionbank/QuestionImportScreen";

export const metadata: Metadata = { title: "CSV로 질문 넣기 · 관리자 · 클래스멘토" };

export default function AdminQuestionsImportPage() {
  return <QuestionImportScreen />;
}
