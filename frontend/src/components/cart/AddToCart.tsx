"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { addCartItem, ApiError } from "@/lib/api";
import { getToken } from "@/lib/auth";

export function AddToCart({ productId, soldOut }: { productId: number; soldOut: boolean }) {
  const router = useRouter();
  const [quantity, setQuantity] = useState(1);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function handleAdd() {
    setMessage(null);
    setError(null);

    // 로그인 화면으로 보내기 전에 미리 걸러 준다
    if (getToken() === null) {
      router.push("/login");
      return;
    }

    try {
      await addCartItem(productId, quantity);
      setMessage("장바구니에 담았습니다");
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        router.push("/login");
        return;
      }
      setError(e instanceof ApiError ? e.message : "담지 못했습니다");
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center gap-3">
        <span className="text-xs tracking-[0.08em]">수량</span>
        <div className="border-line-strong flex h-10 items-center rounded-[var(--radius-field)] border">
          <button
            type="button"
            className="text-muted h-full w-9"
            onClick={() => setQuantity((n) => Math.max(1, n - 1))}
            disabled={soldOut}
          >
            −
          </button>
          <span className="w-8 text-center text-sm">{quantity}</span>
          <button
            type="button"
            className="text-muted h-full w-9"
            onClick={() => setQuantity((n) => Math.min(99, n + 1))}
            disabled={soldOut}
          >
            +
          </button>
        </div>
      </div>

      <button
        type="button"
        onClick={handleAdd}
        disabled={soldOut}
        className="bg-ink h-11 rounded-[var(--radius-field)] text-sm text-white transition-opacity hover:opacity-90 disabled:opacity-40"
      >
        {soldOut ? "품절" : "장바구니 담기"}
      </button>

      {message && <p className="text-muted text-xs">{message}</p>}
      {error && <p className="text-danger text-xs">{error}</p>}
    </div>
  );
}
