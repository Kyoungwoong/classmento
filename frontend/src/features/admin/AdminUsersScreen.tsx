"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { BottomCTA, ButtonLink, ListRow, Screen, ScreenTitle, TopBar } from "@/components/ui";
import { api, type AdminUserView } from "@/lib/api/client";
import { adminError } from "./adminError";

const PROVIDER_LABEL: Record<AdminUserView["provider"], string> = {
  LOCAL: "이메일",
  GOOGLE: "구글",
  KAKAO: "카카오",
};

export function AdminUsersScreen() {
  const router = useRouter();
  const [users, setUsers] = useState<AdminUserView[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .adminUsers()
      .then(setUsers)
      .catch((err) => {
        const message = adminError(err, router, "/admin/users");
        if (message) setError(message);
      });
  }, [router]);

  return (
    <Screen
      header={<TopBar />}
      cta={
        error ? (
          <BottomCTA>
            <ButtonLink href="/">홈으로 가기</ButtonLink>
          </BottomCTA>
        ) : undefined
      }
    >
      {error ? (
        <ScreenTitle title={error} />
      ) : (
        <>
          <ScreenTitle
            title={users ? `회원이 ${users.length}명 있어요` : "회원을 불러오고 있어요"}
            description="최근 가입한 순서예요."
          />
          {users?.length === 0 && (
            <p className="text-body2 text-text-hint">아직 가입한 회원이 없어요</p>
          )}
          {users && users.length > 0 && (
            <ul>
              {users.map((u) => (
                <li key={u.id}>
                  <ListRow
                    title={u.name}
                    trailing={u.role === "ADMIN" ? "관리자" : undefined}
                    description={`${u.email}
${PROVIDER_LABEL[u.provider]} 가입 · ${formatDate(u.createdAt)}`}
                  />
                </li>
              ))}
            </ul>
          )}
        </>
      )}
    </Screen>
  );
}

function formatDate(iso: string) {
  const d = new Date(iso);
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, "0")}.${String(d.getDate()).padStart(2, "0")}`;
}
