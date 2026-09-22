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

async function request<T>(path: string): Promise<T> {
    const res = await fetch(`${BASE_URL}${path}`);
    if (!res.ok) {
      const body = (await res.json().catch(() => null)) as Problem | null;
      throw new ApiError(res.status, body?.code, body?.requestId, body?.detail ?? res.statusText);
    }
    return res.json() as Promise<T>;
}

export function getProducts(categoryId?: number): Promise<Product[]> {
    const query = categoryId ? `?categoryId=${categoryId}` : "";
    return request<Product[]>(`/api/products${query}`);
}
