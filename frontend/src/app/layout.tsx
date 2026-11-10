import type { Metadata, Viewport } from "next";
import { Gaegu } from "next/font/google";
import localFont from "next/font/local";
import { Suspense } from "react";
import { SiteFooter } from "@/components/site/SiteFooter";
import { SiteHeader, SiteHeaderShell } from "@/components/site/SiteHeader";
import "./globals.css";

const pretendard = localFont({
  src: "../../node_modules/pretendard/dist/web/variable/woff2/PretendardVariable.woff2",
  variable: "--font-pretendard",
  weight: "45 920",
  display: "swap",
});

// 손글씨 메모(HandNote)만 쓴다. 한글 글리프는 unicode-range로 필요할 때 받는다
const gaegu = Gaegu({
  weight: "700",
  subsets: ["latin"],
  variable: "--font-gaegu",
  display: "swap",
  preload: false,
});

export const metadata: Metadata = {
  title: "클래스멘토",
  description: "이력서로 준비하는 실전 모의면접",
};

export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  themeColor: "#ffffff",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ko" className={`${pretendard.variable} ${gaegu.variable} h-full antialiased`}>
      <body className="group/body flex min-h-full flex-col bg-bg">
        {/* usePathname은 cacheComponents에서 Suspense 경계가 필요하다(동적 경로) */}
        <Suspense fallback={<SiteHeaderShell />}>
          <SiteHeader />
        </Suspense>
        {/* 기울인 노트·도장 같은 장식이 어떤 폭에서도 페이지를 넓히지 못하게 가로로 자른다.
            clip은 스크롤 상자를 만들지 않아 sticky가 그대로 동작한다 */}
        <div className="flex flex-1 flex-col overflow-x-clip">{children}</div>
        <SiteFooter />
      </body>
    </html>
  );
}
