import { ADMIN_LOGIN_ID, ADMIN_PASSWORD, API_BASE_URL } from "./env";

// 관리자 화면을 거치지 않고 API 로 상품 상태를 만든다. 회원 화면을 보는 테스트의 준비 단계다

async function adminToken(): Promise<string> {
  const res = await fetch(`${API_BASE_URL}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ loginId: ADMIN_LOGIN_ID, password: ADMIN_PASSWORD }),
  });
  return (await res.json()).accessToken;
}

async function adminCall(method: string, path: string, body: unknown) {
  const res = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${await adminToken()}` },
    body: JSON.stringify(body),
  });
  if (!res.ok) {
    throw new Error(`${method} ${path} 실패 ${res.status} ${await res.text()}`);
  }
  return res.status === 204 ? null : res.json();
}

/** 이 테스트만 쓰는 상품을 만든다. 이름을 리턴한다 */
export async function createProduct(stock: number): Promise<string> {
  const name = `E2E 상품 ${Math.random().toString(36).slice(2, 7)}`;
  const categories: { id: number }[] = await fetch(`${API_BASE_URL}/api/admin/categories`, {
    headers: { Authorization: `Bearer ${await adminToken()}` },
  }).then((res) => res.json());

  await adminCall("POST", "/api/admin/products", {
    categoryId: categories[0].id,
    name,
    price: 1500,
    stock,
    thumbnailUrl: "/products/water.jpg",
  });
  return name;
}

async function productId(name: string): Promise<number> {
  const products: { id: number; name: string }[] = await fetch(
    `${API_BASE_URL}/api/admin/products`,
    {
      headers: { Authorization: `Bearer ${await adminToken()}` },
    },
  ).then((res) => res.json());
  return products.find((p) => p.name === name)!.id;
}

export async function changeStock(name: string, stock: number) {
  await adminCall("PATCH", `/api/admin/products/${await productId(name)}/stock`, { stock });
}

export async function endSale(name: string) {
  await adminCall("PATCH", `/api/admin/products/${await productId(name)}/status`, {
    status: "DELETED",
  });
}
