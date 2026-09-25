import { clearToken, getToken } from "@/lib/auth";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

export type ProductStatus = "ON_SALE" | "SOLD_OUT" | "HIDDEN";

// 백엔드 ProductResponse 와 같은 모양
export type Product = {
  id: number;
  categoryId: number;
  name: string;
  price: number;
  status: ProductStatus;
  thumbnailUrl: string | null;
};

export type TokenResponse = {
  accessToken: string;
  expiresIn: number;
};

export type User = {
  id: number;
  loginId: string;
  email: string;
  name: string;
};

// 백엔드 CartResponse 와 같은 모양
export type CartItem = {
  itemId: number;
  productId: number;
  name: string;
  price: number;
  quantity: number;
  lineAmount: number;
  orderable: boolean;
};

export type Cart = {
  items: CartItem[];
  itemAmount: number;
  shippingFee: number;
  totalAmount: number;
  freeShippingRemaining: number;
};

export type OrderStatus = "PENDING" | "PAID" | "SHIPPED" | "FAILED" | "CANCELED";

export type OrderItem = {
  productId: number;
  name: string;
  price: number;
  quantity: number;
  lineAmount: number;
};

export type Order = {
  orderNo: string;
  status: OrderStatus;
  itemAmount: number;
  shippingFee: number;
  totalAmount: number;
  items: OrderItem[];
  receiver: string;
  phone: string;
  zipcode: string;
  addr1: string;
  addr2: string | null;
  createdAt: string;
};

export type PlaceOrderInput = {
  receiver: string;
  phone: string;
  zipcode: string;
  addr1: string;
  addr2?: string;
  saveAddress?: boolean;
};

// 백엔드 ProblemDetail 에서 쓰는 필드
type Problem = {
  status: number;
  detail: string;
  code?: string;
  requestId?: string;
};

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string | undefined,
    readonly requestId: string | undefined,
    message: string,
  ) {
    super(message);
  }
}

type Options = {
  method?: "GET" | "POST" | "PATCH" | "DELETE";
  body?: unknown;
  // 토큰이 필요한 요청. 서버 컴포넌트에서는 못 쓴다
  auth?: boolean;
};

async function request<T>(path: string, options: Options = {}): Promise<T> {
  const { method = "GET", body, auth = false } = options;
  const headers: Record<string, string> = {};
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  if (auth) {
    const token = getToken();
    if (token !== null) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }
  const res = await fetch(`${BASE_URL}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (res.ok) {
    // 202·204 는 본문이 없다. 빈 본문에 res.json() 을 부르면 예외가 난다
    const text = await res.text();
    return (text === "" ? undefined : JSON.parse(text)) as T;
  }

  const problem = (await res.json().catch(() => null)) as Problem | null;
  if (res.status === 401) {
    clearToken();
  }

  throw new ApiError(
    res.status,
    problem?.code,
    problem?.requestId,
    problem?.detail ?? res.statusText,
  );
}

export function getProducts(categoryId?: number): Promise<Product[]> {
  const query = categoryId ? `?categoryId=${categoryId}` : "";
  return request<Product[]>(`/api/products${query}`);
}

export function getProduct(id: number): Promise<Product> {
  return request<Product>(`/api/products/${id}`);
}

export function signUp(input: {
  loginId: string;
  email: string;
  password: string;
  name: string;
}): Promise<void> {
  return request<void>("/api/users", { method: "POST", body: input });
}

export function checkLoginId(loginId: string): Promise<{ available: boolean }> {
  return request<{ available: boolean }>(
    `/api/users/login-id-check?loginId=${encodeURIComponent(loginId)}`,
  );
}

export function login(loginId: string, password: string): Promise<TokenResponse> {
  return request<TokenResponse>("/api/auth/login", {
    method: "POST",
    body: { loginId, password },
  });
}

export function getMe(): Promise<User> {
  return request<User>("/api/users/me", { auth: true });
}

export function resendVerification(email: string): Promise<void> {
  return request<void>("/api/auth/email-verification", { method: "POST", body: { email } });
}

export function confirmVerification(token: string): Promise<void> {
  return request<void>("/api/auth/email-verification/confirm", {
    method: "POST",
    body: { token },
  });
}

export function findLoginId(email: string): Promise<void> {
  return request<void>("/api/auth/login-id/find", { method: "POST", body: { email } });
}

export function requestPasswordReset(email: string): Promise<void> {
  return request<void>("/api/auth/password-reset", { method: "POST", body: { email } });
}

export function resetPassword(token: string, newPassword: string): Promise<void> {
  return request<void>("/api/auth/password-reset/confirm", {
    method: "POST",
    body: { token, newPassword },
  });
}

export function changeName(name: string): Promise<void> {
  return request<void>("/api/users/me", { method: "PATCH", body: { name }, auth: true });
}

export function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  return request<void>("/api/users/me/password", {
    method: "PATCH",
    body: { currentPassword, newPassword },
    auth: true,
  });
}

export function withdraw(password: string): Promise<void> {
  return request<void>("/api/users/me", { method: "DELETE", body: { password }, auth: true });
}

export function getCart(): Promise<Cart> {
  return request<Cart>("/api/cart", { auth: true });
}

export function addCartItem(productId: number, quantity: number): Promise<void> {
  return request<void>("/api/cart/items", {
    method: "POST",
    body: { productId, quantity },
    auth: true,
  });
}

export function changeCartQuantity(itemId: number, quantity: number): Promise<void> {
  return request<void>(`/api/cart/items/${itemId}`, {
    method: "PATCH",
    body: { quantity },
    auth: true,
  });
}

export function removeCartItem(itemId: number): Promise<void> {
  return request<void>(`/api/cart/items/${itemId}`, { method: "DELETE", auth: true });
}

export function placeOrder(
  input: PlaceOrderInput,
): Promise<{ orderNo: string; totalAmount: number }> {
  return request("/api/orders", { method: "POST", body: input, auth: true });
}

export function getOrders(): Promise<Order[]> {
  return request<Order[]>("/api/orders", { auth: true });
}

export function getOrder(orderNo: string): Promise<Order> {
  return request<Order>(`/api/orders/${orderNo}`, { auth: true });
}

export function cancelOrder(orderNo: string): Promise<void> {
  return request<void>(`/api/orders/${orderNo}/cancel`, { method: "POST", auth: true });
}
