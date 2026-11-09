import type { Metadata } from "next";
import { Suspense } from "react";
import { QuestionEditRoute } from "@/features/questionbank/QuestionEditRoute";

export const metadata: Metadata = { title: "질문 수정 · 관리자 · 클래스멘토" };

export default function AdminQuestionsEditPage() {
  // useParams() 같은 URL 훅은 cacheComponents 모드에서 Suspense 경계 안에 있어야 한다.
  return (
    <Suspense>
      <QuestionEditRoute />
    </Suspense>
  );
}
