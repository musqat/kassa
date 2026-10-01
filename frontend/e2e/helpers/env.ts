export const API_BASE_URL = process.env.E2E_API_BASE_URL ?? "http://localhost:8080";
export const MAILPIT_BASE_URL = process.env.E2E_MAILPIT_BASE_URL ?? "http://localhost:8025";

// 관리자 계정은 백엔드가 기동하며 환경 변수로 만든다. 테스트는 그 값으로 로그인만 한다
export const ADMIN_LOGIN_ID = process.env.E2E_ADMIN_LOGIN_ID ?? "admin01";
export const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD ?? "abcd1234";
