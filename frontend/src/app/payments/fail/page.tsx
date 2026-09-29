import Link from "next/link";
import { Suspense } from "react";
import FailMessage from "./FailMessage";

export default function PaymentFailPage() {
  return (
    <main className="mx-auto flex w-full max-w-[560px] flex-col items-start gap-4 px-5 py-14">
      <h1 className="text-lg font-bold tracking-[0.08em]">결제 실패</h1>

      <Suspense fallback={<p className="text-muted text-sm">불러오는 중입니다</p>}>
        <FailMessage />
      </Suspense>

      <Link href="/orders" className="text-muted text-xs underline">
        주문 내역으로
      </Link>
    </main>
  );
}
