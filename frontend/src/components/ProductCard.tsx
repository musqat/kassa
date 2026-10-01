import Link from "next/link";
import { ProductThumb } from "@/components/ProductThumb";
import type { Product } from "@/lib/api";
import { formatPrice } from "@/lib/format";

export function ProductCard({
  product,
  priority = false,
}: {
  product: Product;
  priority?: boolean;
}) {
  const soldOut = product.soldOut;

  return (
    <li>
      <Link href={`/products/${product.id}`} className="flex flex-col gap-2.5">
        <div className="bg-surface text-subtle relative flex aspect-square items-center justify-center overflow-hidden rounded-[var(--radius-card)] text-xs">
          <ProductThumb
            src={product.thumbnailUrl}
            alt={product.name}
            sizes="(min-width: 1024px) 220px, (min-width: 640px) 33vw, 50vw"
            priority={priority}
          />
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
      </Link>
    </li>
  );
}
