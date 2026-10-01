"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import { ApiError, confirmPayment } from "@/lib/api";

// 결제창 인증이 끝나면 paymentKey·orderId·amount 를 쿼리로 달고 이 화면으로 온다
// 승인은 여기서 서버에 건다. 인증은 결제수단 확인까지고 돈은 아직 움직이지 않았다
export default function ConfirmPayment() {
  const router = useRouter();
  const params = useSearchParams();
  const [error, setError] = useState<string | null>(null);

  const orderNo = params.get("orderId");
  const paymentKey = params.get("paymentKey");

  // 리액트가 개발 모드에서 effect 를 두 번 돌린다. 승인을 두 번 걸지 않게 막는다
  const started = useRef(false);

  useEffect(() => {
    if (orderNo === null || paymentKey === null || started.current) {
      return;
    }
    started.current = true;

    // 금액은 쿼리로 온 값을 쓰지 않는다. 서버가 주문 금액으로 다시 확인한다
    confirmPayment(orderNo, paymentKey)
      .then(() => router.replace(`/orders/${orderNo}`))
      .catch((e) => setError(e instanceof ApiError ? e.message : "결제에 실패했습니다"));
  }, [orderNo, paymentKey, router]);

  const message = orderNo === null || paymentKey === null ? "결제 정보가 없습니다" : error;

  if (message === null) {
    return <p className="text-muted text-sm">결제를 확인하고 있습니다</p>;
  }

  return (
    <div className="flex flex-col items-start gap-3">
      <p className="text-danger text-sm">{message}</p>
      <Link href="/orders" className="text-muted text-xs underline">
        주문 내역으로
      </Link>
    </div>
  );
}
