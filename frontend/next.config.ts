import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // 이미지 최적화는 끈다.
  images: { unoptimized: true },

  // 다른 사이트가 iframe 으로 감싸지 못하게 한다. 토큰이 localStorage 라 감싼 화면도 로그인 상태다
  headers() {
    return [
      {
        source: "/:path*",
        headers: [
          { key: "X-Frame-Options", value: "DENY" },
          { key: "Content-Security-Policy", value: "frame-ancestors 'none'" },
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
        ],
      },
    ];
  },
};

export default nextConfig;
