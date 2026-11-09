"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { BottomCTA, ButtonLink, ListRow, Screen, ScreenTitle, TopBar } from "@/components/ui";
import { api } from "@/lib/api/client";
import { adminError } from "./adminError";
import { Chevron } from "./Chevron";

const MENU = [
  { href: "/admin/questions", title: "질문 은행", description: "면접 질문을 넣고 고쳐요" },
  { href: "/admin/categories", title: "카테고리", description: "대분류와 소분류를 정리해요" },
  { href: "/admin/users", title: "회원", description: "가입한 회원을 봐요" },
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

  async function logout() {
    await api.logout().catch(() => undefined);
    router.replace("/");
    router.refresh();
  }

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
    <Screen
      header={
        <TopBar
          back={false}
          trailing={
            <button
              type="button"
              onClick={logout}
              className="-mr-2 h-11 px-2 text-body2 text-text-sub"
            >
              로그아웃
            </button>
          }
        />
      }
    >
      <ScreenTitle
        title={state === "loading" ? "관리 메뉴를 불러오고 있어요" : "무엇을 관리할까요?"}
      />
      {state === "ready" && (
        <ul>
          {MENU.map((m) => (
            <li key={m.href}>
              <ListRow
                title={m.title}
                description={m.description}
                trailing={<Chevron />}
                onClick={() => router.push(m.href)}
              />
            </li>
          ))}
        </ul>
      )}
    </Screen>
  );
}
