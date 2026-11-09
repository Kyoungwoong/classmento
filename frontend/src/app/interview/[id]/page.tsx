import type { Metadata } from "next";
import { Suspense } from "react";
import { InterviewRoute } from "@/features/interview/InterviewRoute";

export const metadata: Metadata = { title: "면접 · 클래스멘토" };

export default function InterviewPage() {
  // useParams() 같은 URL 훅은 cacheComponents 모드에서 Suspense 경계 안에 있어야 한다.
  return (
    <Suspense>
      <InterviewRoute />
    </Suspense>
  );
}
