"use client";

import { useState } from "react";
import type { Product } from "@/lib/api";
import { ProductCard } from "./ProductCard";
import { RecentSidebar } from "./RecentlyViewed";

// 정렬·필터는 받아온 목록에서 처리한다. 페이지네이션이 붙으면 서버로 옮긴다

type Sort = "new" | "priceAsc" | "priceDesc";
type PriceRange = "under10k" | "10kTo30k" | "over30k";

const SORTS: { value: Sort; label: string }[] = [
  { value: "new", label: "신상품순" },
  { value: "priceAsc", label: "낮은 가격순" },
  { value: "priceDesc", label: "높은 가격순" },
];

const PRICE_RANGES: { value: PriceRange; label: string; match: (won: number) => boolean }[] = [
  { value: "under10k", label: "1만원 미만", match: (won) => won < 10_000 },
  { value: "10kTo30k", label: "1만~3만원", match: (won) => won >= 10_000 && won < 30_000 },
  { value: "over30k", label: "3만원 이상", match: (won) => won >= 30_000 },
];

const compare: Record<Sort, (a: Product, b: Product) => number> = {
  // id 는 넣은 순서라 큰 값이 새 상품이다
  new: (a, b) => b.id - a.id,
  priceAsc: (a, b) => a.price - b.price,
  priceDesc: (a, b) => b.price - a.price,
};

const chip = "h-[34px] rounded-full border px-3.5 text-[13px]";
const chipOff = "border-line-strong bg-white text-ink";
const chipOn = "border-ink bg-ink text-white";

export function ProductBrowser({ products }: { products: Product[] }) {
  const [sort, setSort] = useState<Sort>("new");
  const [price, setPrice] = useState<PriceRange | null>(null);
  const [hideSoldOut, setHideSoldOut] = useState(false);

  const range = PRICE_RANGES.find((r) => r.value === price);
  const visible = products
    .filter((p) => !range || range.match(p.price))
    .filter((p) => !hideSoldOut || p.status !== "SOLD_OUT")
    .sort(compare[sort]);

  return (
    <main className="flex flex-grow flex-col gap-8 px-5 py-8 lg:flex-row lg:gap-10 lg:px-12 lg:py-10">
      <aside className="order-1 flex flex-col gap-7 lg:w-[200px] lg:shrink-0 lg:pt-1">
        <div className="flex flex-col gap-3">
          <h2 className="text-[13px] font-bold">정렬</h2>
          <div className="flex flex-col gap-0.5">
            {SORTS.map((s) => {
              const on = sort === s.value;
              return (
                <button
                  key={s.value}
                  type="button"
                  aria-pressed={on}
                  onClick={() => setSort(s.value)}
                  className={`flex h-9 items-center gap-2.5 text-left text-sm ${on ? "text-ink font-bold" : "text-muted"}`}
                >
                  <span className={`size-1.5 rounded-full ${on ? "bg-ink" : ""}`} />
                  {s.label}
                </button>
              );
            })}
          </div>
        </div>

        <div className="bg-line h-px" />

        <div className="flex flex-col gap-3">
          <h2 className="text-[13px] font-bold">가격</h2>
          <div className="flex flex-wrap gap-2">
            {PRICE_RANGES.map((r) => {
              const on = price === r.value;
              return (
                <button
                  key={r.value}
                  type="button"
                  aria-pressed={on}
                  onClick={() => setPrice(on ? null : r.value)}
                  className={`${chip} ${on ? chipOn : chipOff}`}
                >
                  {r.label}
                </button>
              );
            })}
          </div>
        </div>

        <div className="flex flex-col gap-3">
          <h2 className="text-[13px] font-bold">보기</h2>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              aria-pressed={hideSoldOut}
              onClick={() => setHideSoldOut((v) => !v)}
              className={`${chip} ${hideSoldOut ? chipOn : chipOff}`}
            >
              품절 상품 빼기
            </button>
          </div>
        </div>
      </aside>

      <section className="order-3 flex flex-grow flex-col gap-6 lg:order-2">
        <div className="flex items-baseline gap-3">
          <h1 className="text-[22px] font-bold">전체 상품</h1>
          <span className="text-muted text-sm">{visible.length}</span>
        </div>

        {visible.length === 0 ? (
          <p className="text-muted py-20 text-center text-sm">조건에 맞는 상품이 없습니다</p>
        ) : (
          <ul className="grid grid-cols-2 gap-x-4 gap-y-7 sm:grid-cols-3 xl:grid-cols-4">
            {visible.map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </ul>
        )}
      </section>

      <RecentSidebar />
    </main>
  );
}
