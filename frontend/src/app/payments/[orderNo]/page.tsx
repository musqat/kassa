"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import {
  ApiError,
  confirmPayment,
  getOrder,
  resetFakeGateway,
  setFakeAmount,
  setFakeOutcome,
  type FakeOutcome,
  type Order,
} from "@/lib/api";
import { formatPrice } from "@/lib/format";

// 대행사를 붙이기 전이라 결제창 대신 결과를 고르는 버튼을 둔다
// 토스를 붙이면 이 자리에 브라우저 SDK 가 들어간다
type Choice = {
  label: string;
  outcome: FakeOutcome;
  // 금액 불일치를 만들 때만 쓴다
  amount?: number;
  hint: string;
};

const CHOICES: Choice[] = [
  { label: "결제하기", outcome: "SUCCESS", hint: "승인되고 주문이 확정된다" },
  { label: "승인 거절", outcome: "FAIL", hint: "대행사가 거절한다. 주문은 그대로" },
  { label: "응답 없음", outcome: "TIMEOUT", hint: "결과를 모른다. 확인 스케줄러가 3분 뒤 정한다" },
  { label: "금액 조작", outcome: "SUCCESS", amount: 100, hint: "등록 금액과 달라 거절된다" },
];

export default function PaymentPage() {
  const router = useRouter();
  const { orderNo } = useParams<{ orderNo: string }>();
  const [order, setOrder] = useState<Order | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const load = useCallback(
    () =>
      getOrder(orderNo)
        .then(setOrder)
        .catch((e) => {
          if (e instanceof ApiError && e.status === 401) {
            router.replace("/login");
            return;
          }
          setError(e instanceof ApiError ? e.message : "불러오지 못했습니다");
        }),
    [orderNo, router],
  );

  useEffect(() => {
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  // 가짜 게이트웨이 스위치를 맞추고 승인을 건다
  async function pay(choice: Choice) {
    setPending(true);
    setError(null);
    await resetFakeGateway();
    if (choice.amount != null) {
      await setFakeAmount(choice.amount);
    }
    await setFakeOutcome(choice.outcome);
    try {
      await confirmPayment(orderNo);
      router.push(`/orders/${orderNo}`);
    } catch (e) {
      // 타임아웃은 500 이 정상이다. 결제는 진행 중이고 확인 스케줄러가 3분 뒤 정한다
      if (choice.outcome === "TIMEOUT") {
        router.push(`/orders/${orderNo}`);
        return;
      }
      setError(e instanceof ApiError ? e.message : "결제하지 못했습니다");
    } finally {
      setPending(false);
    }
  }

  if (order === null) {
    return (
      <main className="mx-auto w-full max-w-[560px] px-5 py-14">
        <p className="text-muted text-sm">{error ?? "불러오는 중입니다"}</p>
      </main>
    );
  }

  if (order.status !== "PENDING") {
    return (
      <main className="mx-auto flex w-full max-w-[560px] flex-col items-start gap-4 px-5 py-14">
        <h1 className="text-lg font-bold tracking-[0.08em]">결제</h1>
        <p className="text-muted text-sm">이미 처리된 주문입니다</p>
        <Link href={`/orders/${orderNo}`} className="text-xs underline">
          주문 보기
        </Link>
      </main>
    );
  }

  return (
    <main className="mx-auto flex w-full max-w-[560px] flex-col gap-5 px-5 py-10 lg:py-14">
      <header className="flex flex-col gap-1">
        <h1 className="text-lg font-bold tracking-[0.08em]">결제</h1>
        <p className="text-muted text-xs">{order.orderNo}</p>
      </header>

      <section className="border-line flex flex-col gap-3 rounded-[var(--radius-card)] border bg-white px-6 py-6 shadow-[var(--shadow-card)]">
        <div className="flex justify-between text-sm">
          <span className="text-muted">결제 금액</span>
          <span className="text-base font-bold">{formatPrice(order.totalAmount)}</span>
        </div>
      </section>

      <section className="border-line flex flex-col gap-3 rounded-[var(--radius-card)] border bg-white px-6 py-6 shadow-[var(--shadow-card)]">
        <h2 className="text-sm font-bold">결과 고르기</h2>
        <p className="text-subtle text-xs">대행사를 붙이기 전이라 결과를 직접 고릅니다.</p>

        <div className="mt-2 flex flex-col gap-2">
          {CHOICES.map((choice) => (
            <button
              key={choice.label}
              type="button"
              onClick={() => pay(choice)}
              disabled={pending}
              className="border-line flex flex-col items-start gap-1 rounded-[var(--radius-field)] border bg-white px-4 py-3 text-left disabled:opacity-40"
            >
              <span className="text-sm">{choice.label}</span>
              <span className="text-subtle text-xs">{choice.hint}</span>
            </button>
          ))}
        </div>
      </section>

      {error && <p className="text-danger text-xs">{error}</p>}

      <Link href={`/orders/${orderNo}`} className="text-muted px-1 text-xs underline">
        주문으로 돌아가기
      </Link>
    </main>
  );
}
