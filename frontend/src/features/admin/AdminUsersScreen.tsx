"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { BottomCTA, ButtonLink, ListRow, Screen, ScreenTitle, TopBar } from "@/components/ui";
import { api, ApiError, type AdminUserView } from "@/lib/api/client";

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
        if (err instanceof ApiError && err.status === 401) {
          router.replace("/login?next=/admin/users");
          return;
        }
        setError(
          err instanceof ApiError && err.status === 403
            ? "관리자만 볼 수 있어요."
            : "잠시 후 다시 시도해 주세요.",
        );
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
                    title={u.role === "ADMIN" ? `${u.name} · 관리자` : u.name}
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
