"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import { getRole, useRole } from "@/lib/auth";

// 화면을 가리는 것뿐이다. 실제로 막는 건 서버가 한다
export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const role = useRole();

  useEffect(() => {
    // 하이드레이션 첫 렌더의 role 은 아직 null 이다. 여기서는 저장소를 직접 읽는다
    const current = getRole();
    if (current === "ADMIN") {
      return;
    }

    // 로그인을 안 했으면 로그인으로, 권한만 없으면 홈으로
    router.replace(current === null ? "/login" : "/");
  }, [router]);

  if (role !== "ADMIN") {
    return <p className="text-muted px-5 py-14 text-sm">확인 중입니다</p>;
  }

  return (
    <div className="mx-auto flex w-full max-w-[1100px] flex-col gap-6 px-5 py-10">
      <header className="flex items-center gap-4">
        <h1 className="text-lg font-bold tracking-[0.08em]">관리자</h1>
        <nav className="flex gap-3 text-sm">
          <Link href="/admin">상품</Link>
          <Link href="/admin/orders">주문</Link>
        </nav>
      </header>

      {children}
    </div>
  );
}
