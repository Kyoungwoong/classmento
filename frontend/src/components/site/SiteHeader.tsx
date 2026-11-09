"use client";

import clsx from "clsx";
import { Menu, X } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { api, type MeResponse } from "@/lib/api/client";
import { Logo } from "./Logo";

const NAV = [
  { href: "/interview/new", label: "면접 보기" },
  { href: "/resume/upload", label: "이력서 올리기" },
];

/** 로그인·회원가입 화면에서는 그 화면의 CTA가 같은 일을 하므로 헤더 버튼을 숨긴다 */
const AUTH_PATHS = ["/login", "/signup"];

const desktopLink =
  "inline-flex h-11 items-center text-body2 font-medium text-text hover:text-ink aria-[current=page]:font-semibold aria-[current=page]:text-ink";
const mobileLink =
  "flex h-12 w-full items-center text-body1 font-medium text-text aria-[current=page]:font-semibold aria-[current=page]:text-ink";

/**
 * 모든 화면 위의 사이트 헤더: 로고 · 메뉴 · 로그인 상태.
 * 페이지를 옮길 때마다 로그인 상태를 다시 확인한다(로그인·로그아웃 직후 반영).
 */
export function SiteHeader() {
  const router = useRouter();
  const pathname = usePathname();
  const [me, setMe] = useState<MeResponse | null>(null);
  const [open, setOpen] = useState(false);
  const [lastPath, setLastPath] = useState(pathname);
  const toggleRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLElement>(null);

  // 페이지가 바뀌면 메뉴를 닫는다(렌더 중 비교 — effect 안 setState 대신)
  if (lastPath !== pathname) {
    setLastPath(pathname);
    setOpen(false);
  }

  useEffect(() => {
    api
      .me()
      .then(setMe)
      .catch(() => setMe({ authenticated: false }));
  }, [pathname]);

  // 메뉴를 열면 첫 링크로 초점, Escape면 닫고 메뉴 버튼으로 초점을 돌려준다
  useEffect(() => {
    if (!open) return;
    menuRef.current?.querySelector("a")?.focus();
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setOpen(false);
        toggleRef.current?.focus();
      }
    };
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open]);

  async function logout() {
    await api.logout().catch(() => undefined);
    setMe({ authenticated: false });
    router.replace("/");
    router.refresh();
  }

  const isAdmin = me?.user?.role === "ADMIN";
  const links = isAdmin ? [...NAV, { href: "/admin", label: "관리" }] : NAV;
  const current = (href: string) => (pathname.startsWith(href) ? "page" : undefined);
  const onAuthPage = AUTH_PATHS.some((p) => pathname.startsWith(p));

  const account =
    me === null || (onAuthPage && !me.authenticated) ? null : me.authenticated ? (
      <button
        type="button"
        onClick={logout}
        className="inline-flex h-11 items-center px-2 text-body2 font-medium text-text-sub"
      >
        로그아웃
      </button>
    ) : (
      <Link
        href="/login"
        className="inline-flex h-11 items-center rounded-md bg-ink px-4 text-body2 font-semibold text-on-ink"
      >
        로그인
      </Link>
    );

  return (
    <header className="sticky top-0 z-30 border-b border-border bg-bg">
      <div className="mx-auto flex h-16 w-full max-w-site items-center gap-8 px-5 md:px-8">
        <Logo />
        <nav aria-label="주 메뉴" className="hidden flex-1 items-center gap-6 md:flex">
          {links.map((l) => (
            <Link key={l.href} href={l.href} aria-current={current(l.href)} className={desktopLink}>
              {l.label}
            </Link>
          ))}
        </nav>
        <div className="ml-auto flex items-center gap-2 md:ml-0">
          {account}
          <button
            ref={toggleRef}
            type="button"
            aria-expanded={open}
            aria-controls="mobile-menu"
            aria-label={open ? "메뉴 닫기" : "메뉴 열기"}
            onClick={() => setOpen((v) => !v)}
            className="-mr-2 flex size-11 items-center justify-center rounded-sm text-ink md:hidden"
          >
            {open ? <X size={22} aria-hidden /> : <Menu size={22} aria-hidden />}
          </button>
        </div>
      </div>
      {open && (
        <nav
          ref={menuRef}
          id="mobile-menu"
          aria-label="모바일 메뉴"
          className={clsx(
            "absolute inset-x-0 top-full border-b border-border bg-bg px-5 pb-4 shadow-float md:hidden",
          )}
        >
          <ul>
            {links.map((l) => (
              <li key={l.href}>
                <Link href={l.href} aria-current={current(l.href)} className={mobileLink}>
                  {l.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>
      )}
    </header>
  );
}

/** 헤더가 URL을 읽기 전(Suspense fallback)에 보이는 같은 높이의 껍데기 */
export function SiteHeaderShell() {
  return (
    <header className="sticky top-0 z-30 border-b border-border bg-bg">
      <div className="mx-auto flex h-16 w-full max-w-site items-center px-5 md:px-8">
        <Logo />
      </div>
    </header>
  );
}
