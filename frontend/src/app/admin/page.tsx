"use client";

import { useCallback, useEffect, useState } from "react";
import {
  ApiError,
  changeProductStatus,
  changeStock,
  createProduct,
  deleteProduct,
  getAdminProducts,
  getCategories,
  type AdminProduct,
  type Category,
  type ProductStatus,
} from "@/lib/api";
import { formatPrice } from "@/lib/format";

const STATUS_LABEL: Record<ProductStatus, string> = {
  ON_SALE: "판매 중",
  SOLD_OUT: "품절",
  HIDDEN: "숨김",
  DELETED: "판매 종료",
};

// 한 번 받아 두고 거르기는 화면에서 한다. 상품이 수십 개라 왕복할 이유가 없다
type Filter = ProductStatus | "ALL";

export default function AdminProductsPage() {
  const [products, setProducts] = useState<AdminProduct[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [filter, setFilter] = useState<Filter>("ALL");
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(
    () =>
      Promise.all([getAdminProducts(), getCategories()])
        .then(([loadedProducts, loadedCategories]) => {
          setProducts(loadedProducts);
          setCategories(loadedCategories);
        })
        .catch((e) => setError(e instanceof ApiError ? e.message : "불러오기에 실패했습니다")),
    [],
  );

  useEffect(() => {
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  const visible = filter === "ALL" ? products : products.filter((p) => p.status === filter);

  async function handleCreate(form: FormData) {
    const thumbnailUrl = String(form.get("thumbnailUrl") ?? "").trim();

    try {
      await createProduct({
        categoryId: Number(form.get("categoryId")),
        name: String(form.get("name")),
        price: Number(form.get("price")),
        stock: Number(form.get("stock")),
        thumbnailUrl: thumbnailUrl === "" ? null : thumbnailUrl,
      });
      await load();
      setError(null);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "등록에 실패했습니다");
    }
  }

  async function handleStock(product: AdminProduct, stock: number) {
    try {
      const updated = await changeStock(product.id, stock);
      setProducts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
      setError(null);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "재고 수정에 실패했습니다");
      await load();
    }
  }

  async function handleStatus(product: AdminProduct, status: ProductStatus) {
    if (status === "DELETED" && !confirm("판매 종료하면 되돌릴 수 없습니다. 계속할까요?")) {
      await load();
      return;
    }
    try {
      const updated = await changeProductStatus(product.id, status);
      setProducts((prev) => prev.map((p) => (p.id === updated.id ? updated : p)));
      setError(null);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "상태 변경에 실패했습니다");
      await load();
    }
  }

  async function handleDelete(product: AdminProduct) {
    if (!confirm(`${product.name}을 제거 할까요?`)) {
      return;
    }
    try {
      await deleteProduct(product.id);
      setProducts((prev) => prev.filter((p) => p.id !== product.id));
      setError(null);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "제거에 실패했습니다");
    }
  }

  return (
    <section className="flex flex-col gap-5">
      <div className="flex flex-wrap items-center gap-2">
        {(["ALL", "ON_SALE", "SOLD_OUT", "HIDDEN", "DELETED"] as Filter[]).map((value) => (
          <button
            key={value}
            type="button"
            onClick={() => setFilter(value)}
            className={`h-8 rounded-full border px-3 text-[13px] ${
              filter === value ? "border-ink bg-ink text-white" : "border-line-strong bg-white"
            }`}
          >
            {value === "ALL" ? "전체" : STATUS_LABEL[value]}
          </button>
        ))}
        <span className="text-muted ml-auto text-xs">{visible.length}개</span>
      </div>

      {error && <p className="text-danger text-xs">{error}</p>}

      <form
        action={handleCreate}
        className="border-line grid gap-2 rounded-[var(--radius-card)] border bg-white px-5 py-5 sm:grid-cols-[1fr_1fr_120px_100px_auto]"
      >
        <select name="categoryId" className="border-line-strong h-10 rounded border px-2 text-sm">
          {categories.map((category) => (
            <option key={category.id} value={category.id}>
              {category.name}
            </option>
          ))}
        </select>
        <input
          name="name"
          placeholder="상품 이름"
          required
          className="border-line-strong h-10 rounded border px-3 text-sm"
        />
        <input
          name="price"
          type="number"
          placeholder="가격"
          required
          className="border-line-strong h-10 rounded border px-3 text-sm"
        />
        <input
          name="stock"
          type="number"
          placeholder="재고"
          required
          className="border-line-strong h-10 rounded border px-3 text-sm"
        />
        <input
          name="thumbnailUrl"
          placeholder="/products/water.jpg"
          className="border-line-strong h-10 rounded border px-3 text-sm sm:col-span-4"
        />
        <button type="submit" className="bg-ink h-10 rounded px-4 text-sm text-white">
          등록
        </button>
      </form>

      <ul className="flex flex-col gap-2">
        {visible.map((product) => (
          <li
            key={product.id}
            className="border-line flex flex-wrap items-center gap-3 rounded-[var(--radius-card)] border bg-white px-5 py-4 text-sm"
          >
            <span className="min-w-[180px] flex-1">{product.name}</span>
            <span className="text-muted w-[90px]">{product.categoryName}</span>
            <span className="w-[90px] text-right">{formatPrice(product.price)}</span>

            <label className="text-muted flex items-center gap-1 text-xs">
              재고
              <input
                // 서버 값이 바뀌면 다시 그린다. defaultValue 는 처음 그릴 때만 반영된다
                key={product.stock}
                type="number"
                defaultValue={product.stock}
                onBlur={(event) => handleStock(product, Number(event.target.value))}
                className="border-line-strong h-8 w-[70px] rounded border px-2 text-sm"
              />
            </label>
            <span className="text-subtle w-[70px] text-xs">선점 {product.reservedStock}</span>

            <select
              value={product.status}
              onChange={(event) => handleStatus(product, event.target.value as ProductStatus)}
              className="border-line-strong h-8 rounded border px-2 text-xs"
            >
              {(Object.keys(STATUS_LABEL) as ProductStatus[]).map((status) => (
                <option key={status} value={status}>
                  {STATUS_LABEL[status]}
                </option>
              ))}
            </select>

            <button
              type="button"
              onClick={() => handleDelete(product)}
              className="text-danger text-xs underline"
            >
              삭제
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}
