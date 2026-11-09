"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState, type FormEvent } from "react";
import { BottomCTA, Button, Screen, ScreenTitle, TextField, TopBar } from "@/components/ui";
import { api, ApiError } from "@/lib/api/client";
import { safeNext } from "./safeNext";
import { SocialButtons } from "./SocialButtons";

export function SignupForm() {
  const router = useRouter();
  const next = safeNext(useSearchParams().get("next"));

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<{ field: "email" | "password" | "form"; message: string }>();
  const [loading, setLoading] = useState(false);

  // 입력 중에는 조용히, 칸을 벗어났을 때(blur) 짧으면 알려준다
  const [passwordTouched, setPasswordTouched] = useState(false);
  const passwordTooShort = passwordTouched && password.length > 0 && password.length < 8;

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(undefined);
    try {
      await api.signup({ name, email, password });
      router.replace(next);
      router.refresh();
    } catch (err) {
      const message = err instanceof ApiError ? err.message : "잠시 후 다시 시도해 주세요.";
      const field = err instanceof ApiError && err.code === "EMAIL_TAKEN" ? "email" : "form";
      setError({ field, message });
      setLoading(false);
    }
  }

  return (
    <Screen
      width="narrow"
      header={<TopBar />}
      cta={
        <BottomCTA>
          <Button
            type="submit"
            form="signup-form"
            loading={loading}
            loadingText="가입하고 있어요"
            disabled={!name || !email || password.length < 8}
          >
            가입하기
          </Button>
        </BottomCTA>
      }
    >
      <ScreenTitle title={"반가워요\n몇 가지만 알려주세요"} />
      <form id="signup-form" onSubmit={onSubmit} className="flex flex-col gap-5" noValidate>
        <TextField
          label="이름"
          autoComplete="name"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <TextField
          label="이메일"
          type="email"
          autoComplete="email"
          inputMode="email"
          placeholder="example@email.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          error={error?.field === "email" ? error.message : undefined}
        />
        <TextField
          label="비밀번호 (8자 이상)"
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          onBlur={() => setPasswordTouched(true)}
          error={passwordTooShort ? "8자 이상 입력해 주세요." : undefined}
        />
        {error?.field === "form" && (
          <p role="alert" className="text-caption text-danger">
            {error.message}
          </p>
        )}
      </form>
      <p className="mt-4 text-body2 text-text-sub">
        이미 가입하셨나요?{" "}
        <Link
          href={`/login?next=${encodeURIComponent(next)}`}
          className="inline-flex h-11 items-center font-semibold text-accent"
        >
          로그인
        </Link>
      </p>
      <SocialButtons />
    </Screen>
  );
}
