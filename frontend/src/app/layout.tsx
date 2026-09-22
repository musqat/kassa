import type { Metadata } from "next";
import { Noto_Sans_KR } from "next/font/google";
import Link from "next/link";
import "./globals.css";

const notoSansKr = Noto_Sans_KR({
  variable: "--font-noto-sans-kr",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Kassa",
  description: "결제 연동을 다루는 쇼핑몰",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="ko" className={`${notoSansKr.variable} h-full antialiased`}>
      <body className="flex min-h-full flex-col font-sans">
        <header className="flex h-[72px] items-center gap-12 border-b border-line px-12">
          <Link href="/" className="text-xl font-bold tracking-[0.12em]">
            KASSA
          </Link>
          <nav className="flex flex-grow gap-7 text-sm">
            <Link href="/" className="font-bold">
              전체
            </Link>
          </nav>
          <div className="flex gap-6 text-sm">
            <span className="text-muted">장바구니</span>
          </div>
        </header>
        {children}
      </body>
    </html>
  );
}
