"use client";

import Link from "next/link";
import { useEffect, useMemo } from "react";
import { formatPrice } from "@/lib/format";
import { parseRecent, recordRecent, useRecentRaw, type RecentProduct } from "@/lib/recent";

// 상세 화면에서 쓴다. 지금 보는 상품을 기록하고 나머지를 보여준다
export function RecentlyViewed({ product }: { product: RecentProduct }) {
  const raw = useRecentRaw();

  useEffect(() => {
    recordRecent(product);
  }, [product]);

  const items = useMemo(
    () => parseRecent(raw).filter((item) => item.id !== product.id),
    [raw, product.id],
  );

  if (items.length === 0) return null;

  return (
    <section className="border-line flex flex-col gap-3 border-t pt-8">
      <h2 className="text-[13px] font-bold">최근 본 상품</h2>
      <RecentList items={items} />
    </section>
  );
}

// 목록 화면에서 쓴다. 기록하지 않고 보여주기만 한다
export function RecentSidebar() {
  const raw = useRecentRaw();
  const items = useMemo(() => parseRecent(raw), [raw]);

  return (
    <aside className="order-2 flex flex-col gap-4 lg:order-3 lg:w-[200px] lg:shrink-0 lg:pt-1">
      <h2 className="text-[13px] font-bold">최근 본 상품</h2>
      {items.length === 0 ? (
        <p className="text-muted text-[13px]">아직 본 상품이 없습니다</p>
      ) : (
        <RecentList items={items} />
      )}
    </aside>
  );
}

function RecentList({ items }: { items: RecentProduct[] }) {
  return (
    <ul className="flex gap-4 overflow-x-auto lg:flex-col lg:gap-3 lg:overflow-visible">
      {items.map((item) => (
        <li key={item.id}>
          <Link
            href={`/products/${item.id}`}
            className="flex w-[160px] items-center gap-3 lg:w-auto"
          >
            <span className="bg-surface text-subtle flex size-11 shrink-0 items-center justify-center rounded-[var(--radius-field)] text-[10px]">
              이미지
            </span>
            <span className="flex flex-col gap-0.5">
              <span className="text-[13px] leading-snug">{item.name}</span>
              <span className="text-muted text-xs">{formatPrice(item.price)}</span>
            </span>
          </Link>
        </li>
      ))}
    </ul>
  );
}
