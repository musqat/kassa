"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { ApiError, getOrders, type Order } from "@/lib/api";
import { formatDateTime, formatPrice } from "@/lib/format";
import { ORDER_STATUS_LABEL } from "@/lib/order";

export default function OrdersPage() {
  const router = useRouter();
  const [orders, setOrders] = useState<Order[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(
    () =>
      getOrders()
        .then(setOrders)
        .catch((e) => {
          if (e instanceof ApiError && e.status === 401) {
            router.replace("/login");
            return;
          }
          setError(e instanceof ApiError ? e.message : "불러오지 못했습니다");
        }),
    [router],
  );

  useEffect(() => {
    // 토큰이 브라우저에만 있어 화면이 뜬 뒤 가져온다
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  if (orders === null) {
    return (
      <main className="mx-auto w-full max-w-[640px] px-5 py-14">
        <p className="text-muted text-sm">{error ?? "불러오는 중입니다"}</p>
      </main>
    );
  }

  return (
    <main className="mx-auto flex w-full max-w-[640px] flex-col gap-6 px-5 py-10 lg:py-14">
      <h1 className="text-lg font-bold tracking-[0.08em]">주문 내역</h1>

      {orders.length === 0 ? (
        <div className="border-line flex flex-col items-start gap-4 rounded-[var(--radius-card)] border bg-white px-6 py-10 shadow-[var(--shadow-card)]">
          <p className="text-muted text-sm">주문한 상품이 없습니다</p>
          <Link href="/" className="text-xs underline">
            상품 보러 가기
          </Link>
        </div>
      ) : (
        <ul className="flex flex-col gap-3">
          {orders.map((order) => (
            <OrderRow key={order.orderNo} order={order} />
          ))}
        </ul>
      )}
    </main>
  );
}

// 한 줄에 주문번호·날짜·대표 상품·금액·상태를 보여준다
function OrderRow({ order }: { order: Order }) {
  const first = order.items[0];
  const rest = order.items.length - 1;

  return (
    <li>
      <Link
        href={`/orders/${order.orderNo}`}
        className="border-line flex flex-col gap-2 rounded-[var(--radius-card)] border bg-white px-5 py-4 shadow-[var(--shadow-card)]"
      >
        <div className="text-muted flex justify-between text-xs">
          <span>{formatDateTime(order.createdAt)}</span>
          <span>{ORDER_STATUS_LABEL[order.status]}</span>
        </div>
        <div className="flex justify-between gap-3 text-sm">
          <span className="truncate">
            {first.name}
            {rest > 0 && ` 외 ${rest}건`}
          </span>
          <span className="shrink-0 font-bold">{formatPrice(order.totalAmount)}</span>
        </div>
        <span className="text-subtle text-xs">{order.orderNo}</span>
      </Link>
    </li>
  );
}
