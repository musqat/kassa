"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";

// 결제창이 인증 단계에서 실패하면 code 와 message 를 쿼리로 붙여 보낸다
export default function FailMessage() {
  const params = useSearchParams();
  const orderNo = params.get("orderId");
  const message = params.get("message") ?? "결제에 실패했습니다";

  return (
    <div className="flex flex-col items-start gap-3">
      <p className="text-sm">{message}</p>
      <p className="text-subtle text-xs">{params.get("code")}</p>

      {orderNo && (
        <Link href={`/payments/${orderNo}`} className="text-xs underline">
          다시 결제하기
        </Link>
      )}
    </div>
  );
}
