import type { Metadata } from "next";
import { Suspense } from "react";
import { ResumeScreen } from "@/features/resume/ResumeScreen";

export const metadata: Metadata = { title: "이력서 확인 · 클래스멘토" };

export default function ResumePage() {
  // useParams() 같은 URL 훅은 cacheComponents 모드에서 Suspense 경계 안에 있어야 한다.
  return (
    <Suspense>
      <ResumeScreen />
    </Suspense>
  );
}
