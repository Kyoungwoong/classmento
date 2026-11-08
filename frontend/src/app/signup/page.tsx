import type { Metadata } from "next";
import { Suspense } from "react";
import { SignupForm } from "@/features/auth/SignupForm";

export const metadata: Metadata = { title: "회원가입 · 클래스멘토" };

export default function SignupPage() {
  return (
    <Suspense>
      <SignupForm />
    </Suspense>
  );
}
