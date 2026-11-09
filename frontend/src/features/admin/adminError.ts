import { ApiError } from "@/lib/api/client";

interface Navigator {
  replace: (href: string) => void;
}

/**
 * 관리자 화면 공통 오류 처리. 로그인이 풀렸으면(401) 로그인 화면으로 보내고 null을,
 * 그 외에는 화면에 보여줄 문구를 돌려준다.
 */
export function adminError(err: unknown, router: Navigator, next: string): string | null {
  if (err instanceof ApiError && err.status === 401) {
    router.replace(`/login?next=${encodeURIComponent(next)}`);
    return null;
  }
  if (err instanceof ApiError && err.status === 403) return "관리자만 볼 수 있어요.";
  return "잠시 후 다시 시도해 주세요.";
}

/** 저장 같은 행동이 실패했을 때: 서버가 준 문구(원인 + 할 일)를 그대로, 없으면 기본 문구. */
export function actionError(err: unknown): string {
  return err instanceof ApiError ? err.message : "잠시 후 다시 시도해 주세요.";
}
