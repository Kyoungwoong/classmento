import type { Metadata } from "next";
import { UploadScreen } from "@/features/resume/UploadScreen";

export const metadata: Metadata = { title: "이력서 올리기 · 클래스멘토" };

export default function ResumeUploadPage() {
  return <UploadScreen />;
}
