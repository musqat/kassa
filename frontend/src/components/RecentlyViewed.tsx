"use client";

import Link from "next/link";
import { useEffect, useMemo, useSyncExternalStore } from "react";

const KEY = "kassa.recentProducts";
const EVENT = "kassa.recent";
const MAX = 3;

function subscribe(onChange: () => void) {
  window.addEventListener(EVENT, onChange);
  window.addEventListener("storage", onChange);
  return () => {
    window.removeEventListener(EVENT, onChange);
    window.removeEventListener("storage", onChange);
  };
}

// 값이 바뀌었는지 비교할 수 있게 문자열 그대로 돌려준다
function readRaw(): string {
  return localStorage.getItem(KEY) ?? "[]";
}

function parse(raw: string): number[] {
  try {
    return JSON.parse(raw) as number[];
  } catch {
    return [];
  }
}

// 서버를 거치지 않고 브라우저에만 쌓는다
export function RecentlyViewed({ productId }: { productId: number }) {
  const raw = useSyncExternalStore(subscribe, readRaw, () => "[]");

  useEffect(() => {
    const kept = parse(readRaw()).filter((id) => id !== productId);
    localStorage.setItem(KEY, JSON.stringify([productId, ...kept].slice(0, MAX)));
    window.dispatchEvent(new Event(EVENT));
  }, [productId]);

  // 지금 보고 있는 상품은 목록에서 뺀다
  const ids = useMemo(() => parse(raw).filter((id) => id !== productId), [raw, productId]);

  if (ids.length === 0) return null;

  return (
    <section className="border-line flex flex-col gap-3 border-t pt-8">
      <h2 className="text-[13px] font-bold">최근 본 상품</h2>
      <ul className="flex gap-4 text-sm">
        {ids.map((id) => (
          <li key={id}>
            <Link href={`/products/${id}`} className="text-muted underline">
              {id}번 상품
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
