import type { Metadata } from "next";
import { Suspense } from "react";
import { ReportItemRoute } from "@/features/interview/ReportRoutes";

export const metadata: Metadata = { title: "답변 피드백 · 클래스멘토" };

export default function ReportItemPage() {
  return (
    <Suspense>
      <ReportItemRoute />
    </Suspense>
  );
}
