import type { Metadata } from "next";
import { QuestionListScreen } from "@/features/questionbank/QuestionListScreen";

export const metadata: Metadata = { title: "질문 은행 · 관리자 · 클래스멘토" };

export default function AdminQuestionsPage() {
  return <QuestionListScreen />;
}
