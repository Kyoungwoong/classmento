import { NextResponse, type NextRequest } from "next/server";

/**
 * 로그인이 필요한 화면의 "낙관적" 검사: 세션 쿠키가 없으면 로그인 화면으로 보낸다.
 * 진짜 권한 검사는 백엔드(Spring Security)가 한다. 쿠키가 만료된 경우는 화면에서 401을 받아 처리.
 */
export function proxy(request: NextRequest) {
  if (request.cookies.has("SESSION")) {
    return NextResponse.next();
  }
  const login = new URL("/login", request.url);
  login.searchParams.set("next", request.nextUrl.pathname);
  return NextResponse.redirect(login);
}

export const config = {
  matcher: ["/resume/:path*", "/admin/:path*"],
};
