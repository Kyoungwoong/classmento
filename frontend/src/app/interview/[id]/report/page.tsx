import type { Metadata } from "next";
import { Suspense } from "react";
import { ReportRoute } from "@/features/interview/ReportRoutes";

export const metadata: Metadata = { title: "면접 리포트 · 클래스멘토" };

export default function ReportPage() {
  // useParams() 같은 URL 훅은 cacheComponents 모드에서 Suspense 경계 안에 있어야 한다.
  return (
    <Suspense>
      <ReportRoute />
    </Suspense>
  );
}
