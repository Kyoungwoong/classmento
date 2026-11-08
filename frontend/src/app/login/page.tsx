import type { Metadata } from "next";
import { Suspense } from "react";
import { LoginForm } from "@/features/auth/LoginForm";

export const metadata: Metadata = { title: "로그인 · 클래스멘토" };

export default function LoginPage() {
  // useSearchParams를 쓰는 클라이언트 컴포넌트는 Suspense 경계가 필요하다(cacheComponents).
  return (
    <Suspense>
      <LoginForm />
    </Suspense>
  );
}
