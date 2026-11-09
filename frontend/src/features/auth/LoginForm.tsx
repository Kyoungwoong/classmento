"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState, type FormEvent } from "react";
import { Mascot } from "@/components/site/Mascot";
import { BottomCTA, Button, Panel, Screen, ScreenTitle, TextField, TopBar } from "@/components/ui";
import { api, ApiError } from "@/lib/api/client";
import { safeNext } from "./safeNext";
import { SocialButtons } from "./SocialButtons";

export function LoginForm() {
  const router = useRouter();
  const params = useSearchParams();
  const next = safeNext(params.get("next"));
  const socialError = params.get("error") === "social";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  // 비밀번호 틀림은 비밀번호 칸 아래, 그 외(네트워크·소셜 실패)는 폼 아래에 보여준다
  const [error, setError] = useState<{ field: "password" | "form"; message: string } | null>(
    socialError
      ? { field: "form", message: "간편 로그인에 실패했어요. 이메일로 로그인해 주세요." }
      : null,
  );
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await api.login({ email, password });
      router.replace(next);
      router.refresh();
    } catch (err) {
      const isWrongPassword = err instanceof ApiError && err.code === "LOGIN_FAILED";
      setError({
        field: isWrongPassword ? "password" : "form",
        message:
          err instanceof ApiError ? err.message : "연결이 불안정해요. 잠시 후 다시 시도해 주세요.",
      });
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
            form="login-form"
            loading={loading}
            loadingText="로그인하고 있어요"
            disabled={!email || !password}
          >
            로그인하기
          </Button>
        </BottomCTA>
      }
    >
      <div className="flex items-end justify-between gap-4">
        <ScreenTitle title={"이메일로\n로그인해 주세요"} />
        <Mascot mood="idle" size={72} className="mb-8 shrink-0 md:mb-10" />
      </div>
      <Panel>
        <form id="login-form" onSubmit={onSubmit} className="flex flex-col gap-5" noValidate>
          <TextField
            label="이메일"
            type="email"
            autoComplete="email"
            inputMode="email"
            placeholder="example@email.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />
          <TextField
            label="비밀번호"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={error?.field === "password" ? error.message : undefined}
          />
          {error?.field === "form" && (
            <p role="alert" className="text-caption text-danger">
              {error.message}
            </p>
          )}
        </form>
        <p className="mt-4 text-body2 text-text-sub">
          처음이신가요?{" "}
          <Link
            href={`/signup?next=${encodeURIComponent(next)}`}
            className="inline-flex h-11 items-center font-semibold text-accent"
          >
            회원가입
          </Link>
        </p>
        <SocialButtons />
      </Panel>
    </Screen>
  );
}
