import type { Metadata } from "next";
import { NewInterviewScreen } from "@/features/interview/NewInterviewScreen";

export const metadata: Metadata = { title: "면접 만들기 · 클래스멘토" };

export default function NewInterviewPage() {
  return <NewInterviewScreen />;
}
