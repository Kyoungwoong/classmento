/** 로그인 후 돌아갈 경로. 외부 주소로 튕기는 오픈 리다이렉트를 막기 위해 우리 사이트 내부 경로만 허용한다. */
export function safeNext(next: string | null | undefined, fallback = "/"): string {
  if (!next || !next.startsWith("/") || next.startsWith("//") || next.startsWith("/\\")) {
    return fallback;
  }
  return next;
}
