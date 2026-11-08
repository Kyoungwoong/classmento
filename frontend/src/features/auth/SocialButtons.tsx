"use client";

import { useEffect, useState } from "react";
import { api, type SocialProvider } from "@/lib/api/client";

/** 백엔드에 키가 설정된 소셜 로그인만 보여준다. 하나도 없으면 아무것도 그리지 않는다. */
export function SocialButtons() {
  const [providers, setProviders] = useState<SocialProvider[]>([]);

  useEffect(() => {
    api
      .providers()
      .then((r) => setProviders(r.providers))
      .catch(() => setProviders([]));
  }, []);

  if (providers.length === 0) return null;

  return (
    <div className="mt-10">
      <p className="mb-3 text-caption text-text-sub">간편하게 시작하기</p>
      <div className="flex flex-col gap-2">
        {providers.map((p) => (
          <a
            key={p.id}
            href={`/oauth2/authorization/${p.id}`}
            className="flex h-12 items-center justify-center rounded-md bg-bg-muted text-body2 font-semibold text-text-strong active:bg-gray-200"
          >
            {p.name}로 계속하기
          </a>
        ))}
      </div>
    </div>
  );
}
