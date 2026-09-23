"use client";

import { useSyncExternalStore } from "react";

const KEY = "kassa.recentProducts";
const EVENT = "kassa.recent";
const MAX = 3;

// 이름·가격까지 담아 두면 목록을 그릴 때 서버를 부르지 않아도 된다
export type RecentProduct = {
  id: number;
  name: string;
  price: number;
};

function subscribe(onChange: () => void) {
  window.addEventListener(EVENT, onChange);
  window.addEventListener("storage", onChange);
  return () => {
    window.removeEventListener(EVENT, onChange);
    window.removeEventListener("storage", onChange);
  };
}

// 바뀌었는지 비교할 수 있게 문자열 그대로 돌려준다
function readRaw(): string {
  try {
    return localStorage.getItem(KEY) ?? "[]";
  } catch {
    return "[]";
  }
}

export function parseRecent(raw: string): RecentProduct[] {
  try {
    const parsed = JSON.parse(raw) as unknown;
    if (!Array.isArray(parsed)) return [];
    // 옛 기록은 숫자 배열이었다. 모양이 다른 값은 버린다
    return parsed.filter(
      (item): item is RecentProduct =>
        typeof item === "object" && item !== null && "id" in item && "name" in item,
    );
  } catch {
    return [];
  }
}

export function recordRecent(product: RecentProduct) {
  const kept = parseRecent(readRaw()).filter((item) => item.id !== product.id);
  localStorage.setItem(KEY, JSON.stringify([product, ...kept].slice(0, MAX)));
  window.dispatchEvent(new Event(EVENT));
}

export function useRecentRaw(): string {
  return useSyncExternalStore(subscribe, readRaw, () => "[]");
}
