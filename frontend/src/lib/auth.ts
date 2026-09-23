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

export function isLoggedIn(): boolean {
  return getToken() !== null;
}
