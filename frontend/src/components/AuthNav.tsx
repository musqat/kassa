"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useSyncExternalStore } from "react";
import { AUTH_EVENT, clearToken, getToken } from "@/lib/auth";

// 토큰이 바뀌면 다시 그린다. 다른 탭의 변화는 storage 이벤트로 온다
function subscribe(onChange: () => void) {
  window.addEventListener(AUTH_EVENT, onChange);
  window.addEventListener("storage", onChange);
  return () => {
    window.removeEventListener(AUTH_EVENT, onChange);
    window.removeEventListener("storage", onChange);
  };
}

export function AuthNav() {
  const router = useRouter();
  // 서버에서 그릴 때는 로그아웃 상태
  const loggedIn = useSyncExternalStore(
    subscribe,
    () => getToken() !== null,
    () => false,
  );

  if (!loggedIn) {
    return (
      <Link href="/login" className="text-muted">
        로그인
      </Link>
    );
  }

  return (
    <span className="flex gap-6">
      <Link href="/me" className="text-muted">
        내 정보
      </Link>
      <button
        type="button"
        className="text-muted"
        onClick={() => {
          clearToken();
          router.push("/");
        }}
      >
        로그아웃
      </button>
    </span>
  );
}
