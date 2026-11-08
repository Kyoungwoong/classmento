import type { NextConfig } from "next";

/** 백엔드 주소. 브라우저는 항상 같은 출처(/api)로 부르고 Next가 여기로 넘긴다 → 쿠키·CORS 문제 없음. */
const BACKEND_URL = process.env.BACKEND_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  // Docker 이미지용: 실행에 필요한 파일만 .next/standalone에 모은다 (frontend/Dockerfile)
  output: "standalone",
  cacheComponents: true,
  partialPrefetching: true,
  experimental: {
    // 이력서 업로드(최대 20MB)가 프록시를 통과하도록. 기본값보다 크게.
    proxyClientMaxBodySize: "25mb",
  },
  async rewrites() {
    return [
      { source: "/api/:path*", destination: `${BACKEND_URL}/api/:path*` },
      // 소셜 로그인(Spring Security OAuth2) 경로
      { source: "/oauth2/:path*", destination: `${BACKEND_URL}/oauth2/:path*` },
      { source: "/login/oauth2/:path*", destination: `${BACKEND_URL}/login/oauth2/:path*` },
    ];
  },
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
