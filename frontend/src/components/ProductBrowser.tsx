"use client";

import { useState } from "react";
import type { Product } from "@/lib/api";
import { ProductCard } from "./ProductCard";

// 정렬·필터는 받아온 목록에서 처리한다. 상품이 적은 동안만 맞는 방식이고,
// 페이지네이션이 붙으면 서버 파라미터로 옮긴다.

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

const chip = "h-[34px] border px-3 text-[13px]";
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
    <main className="flex flex-grow gap-10 px-12 py-10">
      <aside className="flex w-[200px] shrink-0 flex-col gap-7 pt-1">
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
                  className={`flex h-9 items-center gap-2.5 text-left text-sm ${on ? "font-bold text-ink" : "text-muted"}`}
                >
                  <span className={`size-1.5 rounded-full ${on ? "bg-ink" : ""}`} />
                  {s.label}
                </button>
              );
            })}
          </div>
        </div>

        <div className="h-px bg-line" />

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

      <section className="flex flex-grow flex-col gap-6">
        <div className="flex items-baseline gap-3">
          <h1 className="text-[22px] font-bold">전체 상품</h1>
          <span className="text-sm text-muted">{visible.length}</span>
        </div>

        {visible.length === 0 ? (
          <p className="py-20 text-center text-sm text-muted">조건에 맞는 상품이 없습니다</p>
        ) : (
          <ul className="grid grid-cols-4 gap-x-5 gap-y-8">
            {visible.map((p) => (
              <ProductCard key={p.id} product={p} />
            ))}
          </ul>
        )}
      </section>

      <aside className="flex w-[200px] shrink-0 flex-col gap-4 pt-1">
        <h2 className="text-[13px] font-bold">최근 본 상품</h2>
        <p className="text-[13px] text-muted">아직 본 상품이 없습니다</p>
      </aside>
    </main>
  );
}
