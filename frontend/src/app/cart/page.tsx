"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import {
  ApiError,
  changeCartQuantity,
  getCart,
  removeCartItem,
  type Cart,
  type CartItem,
} from "@/lib/api";
import { formatPrice } from "@/lib/format";

export default function CartPage() {
  const router = useRouter();
  const [cart, setCart] = useState<Cart | null>(null);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(
    () =>
      getCart()
        .then(setCart)
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
    // 화면이 뜬 뒤 한 번 가져온다. 토큰이 브라우저에만 있어 서버에서는 못 부른다
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  async function change(itemId: number, quantity: number) {
    setError(null);
    try {
      await changeCartQuantity(itemId, quantity);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "수량을 바꾸지 못했습니다");
    }
    await load();
  }

  async function remove(itemId: number) {
    setError(null);
    await removeCartItem(itemId).catch(() => undefined);
    await load();
  }

  if (cart === null) {
    return (
      <main className="mx-auto w-full max-w-[720px] px-5 py-14">
        <p className="text-muted text-sm">{error ?? "불러오는 중입니다"}</p>
      </main>
    );
  }

  return (
    <main className="mx-auto flex w-full max-w-[720px] flex-col gap-6 px-5 py-10 lg:py-14">
      <h1 className="text-lg font-bold tracking-[0.08em]">장바구니</h1>

      {cart.items.length === 0 ? (
        <div className="border-line flex flex-col items-start gap-4 rounded-[var(--radius-card)] border bg-white px-6 py-10 shadow-[var(--shadow-card)]">
          <p className="text-muted text-sm">담은 상품이 없습니다</p>
          <Link href="/" className="text-xs underline">
            상품 보러 가기
          </Link>
        </div>
      ) : (
        <>
          <ul className="flex flex-col gap-3">
            {cart.items.map((item) => (
              <CartRow key={item.itemId} item={item} onChange={change} onRemove={remove} />
            ))}
          </ul>

          {error && <p className="text-danger text-xs">{error}</p>}

          <section className="border-line flex flex-col gap-3 rounded-[var(--radius-card)] border bg-white px-6 py-6 text-sm shadow-[var(--shadow-card)]">
            <div className="flex justify-between">
              <span className="text-muted">상품 금액</span>
              <span>{formatPrice(cart.itemAmount)}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-muted">배송비</span>
              <span>{formatPrice(cart.shippingFee)}</span>
            </div>
            {cart.freeShippingRemaining > 0 && (
              <p className="text-subtle text-xs">
                {formatPrice(cart.freeShippingRemaining)} 더 담으면 무료배송입니다
              </p>
            )}
            <div className="border-line mt-1 flex justify-between border-t border-dashed pt-3 font-bold">
              <span>결제 예정 금액</span>
              <span>{formatPrice(cart.totalAmount)}</span>
            </div>
          </section>

          <button
            type="button"
            disabled
            className="bg-ink h-12 rounded-[var(--radius-field)] text-sm text-white disabled:opacity-40"
          >
            주문하기
          </button>
        </>
      )}
    </main>
  );
}

type RowProps = {
  item: CartItem;
  onChange: (itemId: number, quantity: number) => void;
  onRemove: (itemId: number) => void;
};

function CartRow({ item, onChange, onRemove }: RowProps) {
  return (
    <li
      className={`border-line flex items-center gap-4 rounded-[var(--radius-card)] border bg-white px-4 py-4 shadow-[var(--shadow-card)] ${
        item.orderable ? "" : "opacity-60"
      }`}
    >
      <div className="bg-surface text-subtle flex size-16 shrink-0 items-center justify-center rounded-[var(--radius-field)] text-[10px]">
        이미지
      </div>

      <div className="flex flex-1 flex-col gap-1">
        <Link href={`/products/${item.productId}`} className="text-sm">
          {item.name}
        </Link>
        <span className="text-muted text-xs">{formatPrice(item.price)}</span>
        {!item.orderable && <span className="text-subtle text-xs">품절되어 결제에서 빠집니다</span>}
      </div>

      <div className="flex flex-col items-end gap-2">
        <div className="border-line-strong flex h-9 items-center rounded-[var(--radius-field)] border">
          <button
            type="button"
            className="text-muted h-full w-8"
            onClick={() => onChange(item.itemId, item.quantity - 1)}
            disabled={item.quantity <= 1}
          >
            −
          </button>
          <span className="w-7 text-center text-sm">{item.quantity}</span>
          <button
            type="button"
            className="text-muted h-full w-8"
            onClick={() => onChange(item.itemId, item.quantity + 1)}
            disabled={item.quantity >= 99}
          >
            +
          </button>
        </div>
        <span className="text-sm font-bold">{formatPrice(item.lineAmount)}</span>
      </div>

      <button
        type="button"
        onClick={() => onRemove(item.itemId)}
        className="text-subtle self-start text-xs"
        aria-label="삭제"
      >
        ✕
      </button>
    </li>
  );
}
