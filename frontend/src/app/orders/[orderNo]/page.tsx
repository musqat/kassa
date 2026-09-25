"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { ApiError, cancelOrder, getOrder, type Order } from "@/lib/api";
import { formatDateTime, formatPrice } from "@/lib/format";
import { ORDER_STATUS_LABEL } from "@/lib/order";

export default function OrderDetailPage() {
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
    // 토큰이 브라우저에만 있어 화면이 뜬 뒤 가져온다
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  async function handleCancel() {
    setError(null);
    setPending(true);

    try {
      await cancelOrder(orderNo);
      // 취소 시각까지 서버가 채워 주므로 상태만 바꾸지 않고 다시 읽는다
      await load();
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        router.replace("/login");
        return;
      }
      setError(e instanceof ApiError ? e.message : "취소하지 못했습니다");
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

  return (
    <main className="mx-auto flex w-full max-w-[560px] flex-col gap-5 px-5 py-10 lg:py-14">
      <header className="flex flex-col gap-1">
        <h1 className="text-lg font-bold tracking-[0.08em]">주문 상세</h1>
        <p className="text-muted text-xs">
          {order.orderNo} · {formatDateTime(order.createdAt)} · {ORDER_STATUS_LABEL[order.status]}
        </p>
      </header>

      <section className="border-line flex flex-col gap-3 rounded-[var(--radius-card)] border bg-white px-6 py-6 shadow-[var(--shadow-card)]">
        <h2 className="text-sm font-bold">주문 상품</h2>
        <ul className="flex flex-col gap-2 text-sm">
          {order.items.map((item) => (
            <li key={item.productId} className="flex justify-between gap-3">
              <span className="truncate">
                {item.name} × {item.quantity}
              </span>
              <span className="shrink-0">{formatPrice(item.lineAmount)}</span>
            </li>
          ))}
        </ul>

        <div className="border-line mt-2 flex flex-col gap-2 border-t pt-3 text-sm">
          <div className="flex justify-between">
            <span className="text-muted">상품 금액</span>
            <span>{formatPrice(order.itemAmount)}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted">배송비</span>
            <span>{formatPrice(order.shippingFee)}</span>
          </div>
          <div className="border-line flex justify-between border-t border-dashed pt-2 font-bold">
            <span>결제 금액</span>
            <span>{formatPrice(order.totalAmount)}</span>
          </div>
        </div>
      </section>

      <section className="border-line flex flex-col gap-2 rounded-[var(--radius-card)] border bg-white px-6 py-6 text-sm shadow-[var(--shadow-card)]">
        <h2 className="text-sm font-bold">배송지</h2>
        <p>
          {order.receiver} · {order.phone}
        </p>
        <p className="text-muted">
          ({order.zipcode}) {order.addr1}
          {order.addr2 && ` ${order.addr2}`}
        </p>
      </section>

      {error && <p className="text-danger text-xs">{error}</p>}

      {order.status === "PENDING" && (
        <button
          type="button"
          onClick={handleCancel}
          disabled={pending}
          className="border-line text-danger h-12 rounded-[var(--radius-field)] border bg-white text-sm disabled:opacity-40"
        >
          주문 취소
        </button>
      )}

      <Link href="/orders" className="text-muted px-1 text-xs underline">
        주문 내역으로
      </Link>
    </main>
  );
}
