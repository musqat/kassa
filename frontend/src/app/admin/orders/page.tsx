"use client";

import { useCallback, useEffect, useState } from "react";
import {
  ApiError,
  getAdminOrder,
  getAdminOrders,
  shipOrder,
  type AdminOrder,
  type AdminOrderDetail,
  type OrderStatus,
} from "@/lib/api";
import { formatDateTime, formatPrice } from "@/lib/format";
import { ORDER_STATUS_LABEL } from "@/lib/order";

type Filter = OrderStatus | "ALL";

const FILTERS: Filter[] = ["ALL", "PENDING", "PAID", "SHIPPED", "CANCELED", "FAILED"];

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<AdminOrder[]>([]);
  const [filter, setFilter] = useState<Filter>("ALL");
  const [opened, setOpened] = useState<AdminOrderDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(
    () =>
      getAdminOrders()
        .then(setOrders)
        .catch((e) => setError(e instanceof ApiError ? e.message : "불러오기에 실패했습니다")),
    [],
  );

  useEffect(() => {
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  const visible = filter === "ALL" ? orders : orders.filter((o) => o.status === filter);

  async function toggle(orderNo: string) {
    if (opened?.order.orderNo === orderNo) {
      setOpened(null);
      return;
    }

    try {
      setOpened(await getAdminOrder(orderNo));
      setError(null);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "불러오기에 실패했습니다");
    }
  }

  async function handleShip(order: AdminOrder) {
    try {
      const updated = await shipOrder(order.orderNo);
      setOrders((prev) => prev.map((o) => (o.orderNo === updated.orderNo ? updated : o)));
      setError(null);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "배송 처리에 실패했습니다");
      await load();
    }
  }

  return (
    <section className="flex flex-col gap-5">
      <div className="flex flex-wrap items-center gap-2">
        {FILTERS.map((value) => (
          <button
            key={value}
            type="button"
            onClick={() => setFilter(value)}
            className={`h-8 rounded-full border px-3 text-[13px] ${
              filter === value ? "border-ink bg-ink text-white" : "border-line-strong bg-white"
            }`}
          >
            {value === "ALL" ? "전체" : ORDER_STATUS_LABEL[value]}
          </button>
        ))}
        <span className="text-muted ml-auto text-xs">{visible.length}건</span>
      </div>

      {error && <p className="text-danger text-xs">{error}</p>}

      <ul className="flex flex-col gap-2">
        {visible.map((order) => (
          <li
            key={order.orderNo}
            className="border-line flex flex-col gap-3 rounded-[var(--radius-card)] border bg-white px-5 py-4 text-sm"
          >
            <div className="flex flex-wrap items-center gap-3">
              <button
                type="button"
                onClick={() => toggle(order.orderNo)}
                className="min-w-[170px] text-left underline"
              >
                {order.orderNo}
              </button>
              <span className="text-muted w-[90px]">{order.loginId ?? "탈퇴 회원"}</span>
              <span className="w-[80px]">{order.receiver}</span>
              <span className="w-[90px] text-right">{formatPrice(order.totalAmount)}</span>
              <span className="text-muted w-[80px] text-xs">
                {ORDER_STATUS_LABEL[order.status]}
              </span>
              <span className="text-subtle text-xs">{formatDateTime(order.createdAt)}</span>

              {order.status === "PAID" && (
                <button
                  type="button"
                  onClick={() => handleShip(order)}
                  className="border-line ml-auto h-8 rounded border px-3 text-xs"
                >
                  배송 처리
                </button>
              )}
            </div>

            {opened?.order.orderNo === order.orderNo && (
              <div className="border-line text-muted flex flex-col gap-1 border-t pt-3 text-xs">
                <p>{opened.items.map((item) => `${item.name} × ${item.quantity}`).join(", ")}</p>
                <p>
                  {opened.phone} · ({opened.zipcode}) {opened.addr1}
                  {opened.addr2 && ` ${opened.addr2}`}
                </p>
                <p>
                  결제 {opened.method === "CARD" ? "카드" : (opened.method ?? "-")}
                  {opened.order.paidAt && ` · ${formatDateTime(opened.order.paidAt)}`}
                  {opened.order.shippedAt && ` · 배송 ${formatDateTime(opened.order.shippedAt)}`}
                </p>
              </div>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
