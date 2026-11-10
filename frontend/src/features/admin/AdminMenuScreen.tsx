"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { FolderTree, Library, Users } from "lucide-react";
import { BottomCTA, ButtonLink, ListRow, Panel, Screen, ScreenTitle } from "@/components/ui";
import { api } from "@/lib/api/client";
import { adminError } from "./adminError";
import { Chevron } from "./Chevron";

const MENU = [
  {
    href: "/admin/questions",
    title: "질문 은행",
    description: "면접 질문을 넣고 고쳐요",
    icon: Library,
  },
  {
    href: "/admin/categories",
    title: "카테고리",
    description: "대분류와 소분류를 정리해요",
    icon: FolderTree,
  },
  { href: "/admin/users", title: "회원", description: "가입한 회원을 봐요", icon: Users },
] as const;

/** /admin — 관리 메뉴. 관리자만. */
export function AdminMenuScreen() {
  const router = useRouter();
  const [state, setState] = useState<"loading" | "ready" | { error: string }>("loading");

  useEffect(() => {
    api
      .me()
      .then((me) => {
        if (!me.authenticated) {
          router.replace("/login?next=/admin");
        } else {
          setState(me.user?.role === "ADMIN" ? "ready" : { error: "관리자만 볼 수 있어요." });
        }
      })
      .catch((err) => {
        const message = adminError(err, router, "/admin");
        if (message) setState({ error: message });
      });
  }, [router]);

  if (typeof state === "object") {
    return (
      <Screen
        cta={
          <BottomCTA>
            <ButtonLink href="/">홈으로 가기</ButtonLink>
          </BottomCTA>
        }
      >
        <ScreenTitle title={state.error} />
      </Screen>
    );
  }

  return (
    // 로그아웃은 사이트 헤더에 있다(같은 버튼을 두 번 두지 않는다)
    <Screen>
      <ScreenTitle
        title={state === "loading" ? "관리 메뉴를 불러오고 있어요" : "무엇을 관리할까요?"}
      />
      {state === "ready" && (
        <Panel>
          <ul className="divide-y divide-border">
            {MENU.map((m) => (
              <li key={m.href}>
                <ListRow
                  leading={
                    <span className="flex size-10 items-center justify-center rounded-md bg-bg-tint text-ink">
                      <m.icon size={20} aria-hidden />
                    </span>
                  }
                  title={m.title}
                  description={m.description}
                  trailing={<Chevron />}
                  onClick={() => router.push(m.href)}
                />
              </li>
            ))}
          </ul>
        </Panel>
      )}
    </Screen>
  );
}
