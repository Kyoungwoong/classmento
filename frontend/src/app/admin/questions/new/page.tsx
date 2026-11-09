import type { Metadata } from "next";
import { QuestionFormScreen } from "@/features/questionbank/QuestionFormScreen";

export const metadata: Metadata = { title: "질문 추가 · 관리자 · 클래스멘토" };

export default function AdminQuestionsNewPage() {
  return <QuestionFormScreen />;
}
