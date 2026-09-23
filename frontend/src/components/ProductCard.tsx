import type { Product } from "@/lib/api";
import { formatPrice } from "@/lib/format";

export function ProductCard({ product }: { product: Product }) {
  const soldOut = product.status === "SOLD_OUT";

  return (
    <li className="flex flex-col gap-2.5">
      <div className="bg-surface text-subtle relative flex aspect-square items-center justify-center rounded-[var(--radius-card)] text-xs">
        상품 이미지
        {soldOut && (
          <div className="text-ink absolute inset-0 flex items-center justify-center rounded-[var(--radius-card)] bg-white/70 text-xs font-bold tracking-[0.18em]">
            SOLD OUT
          </div>
        )}
      </div>
      <div className={`flex flex-col gap-1 ${soldOut ? "text-subtle" : ""}`}>
        <span className="text-sm leading-snug">{product.name}</span>
        <span className="text-[15px] font-bold">{formatPrice(product.price)}</span>
      </div>
    </li>
  );
}
