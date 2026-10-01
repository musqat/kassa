"use client";

import { useSyncExternalStore } from "react";

// 토큰은 브라우저에만 둔다. 서버 컴포넌트에서는 못 쓴다
// 같은 탭의 변화를 알리는 이벤트
export const AUTH_EVENT = "kassa.auth";

const TOKEN_KEY = "kassa.accessToken";
const EXPIRES_KEY = "kassa.expiresAt";

export function saveToken(token: string, expiresIn: number): void {
  const expiresAt = Date.now() + expiresIn * 1000;
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(EXPIRES_KEY, String(expiresAt));
  window.dispatchEvent(new Event(AUTH_EVENT));
}

export function getToken(): string | null {
  const token = localStorage.getItem(TOKEN_KEY);
  const expiresAt = localStorage.getItem(EXPIRES_KEY);

  if (token === null || expiresAt === null) {
    return null;
  }

  if (Number(expiresAt) <= Date.now()) {
    clearToken();
    return null;
  }

  return token;
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(EXPIRES_KEY);
  window.dispatchEvent(new Event(AUTH_EVENT));
}

export type Role = "USER" | "ADMIN";

// 토큰 안의 권한을 읽는다. 메뉴를 보여줄지 정하는 용도이고, 막는 건 서버가 한다
export function getRole(): Role | null {
  const token = getToken();
  if (token == null) return null;

  try {
    const payload = token.split(".")[1];
    const json = atob(payload.replace(/-/g, "+").replace(/_/g, "/"));
    const role = JSON.parse(json).role;

    return role === "ADMIN" || role === "USER" ? role : null;
  } catch {
    return null;
  }
}

export function isLoggedIn(): boolean {
  return getToken() !== null;
}

// 토큰이 바뀌면 다시 읽는다. 서버에서 그릴 때는 권한을 모른다
function subscribeAuth(onChange: () => void) {
  window.addEventListener(AUTH_EVENT, onChange);
  window.addEventListener("storage", onChange);
  return () => {
    window.removeEventListener(AUTH_EVENT, onChange);
    window.removeEventListener("storage", onChange);
  };
}

export function useRole(): Role | null {
  return useSyncExternalStore(
    subscribeAuth,
    () => getRole(),
    () => null,
  );
}
