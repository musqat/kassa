import { connection } from "next/server";
import { notFound } from "next/navigation";
import { AddToCart } from "@/components/cart/AddToCart";
import { RecentlyViewed } from "@/components/RecentlyViewed";
import { ApiError, getProduct } from "@/lib/api";
import { formatPrice } from "@/lib/format";

// 가격·재고 상태가 바뀌니 요청마다 가져온다
export default async function ProductPage({ params }: PageProps<"/products/[id]">) {
  await connection();

  const { id } = await params;
  const product = await getProduct(Number(id)).catch((e) => {
    if (e instanceof ApiError && e.status === 404) notFound();
    throw e;
  });

  const soldOut = product.status !== "ON_SALE";

  return (
    <main className="mx-auto flex w-full max-w-[900px] flex-col gap-10 px-5 py-10 lg:px-12">
      <div className="flex flex-col gap-8 sm:flex-row sm:gap-10">
        <div className="bg-surface text-subtle flex aspect-square w-full shrink-0 items-center justify-center rounded-[var(--radius-card)] text-xs sm:w-[380px]">
          상품 이미지
        </div>

        <div className="flex flex-1 flex-col gap-6">
          <div className="flex flex-col gap-2">
            <h1 className="text-xl font-bold">{product.name}</h1>
            <p className="text-[22px] font-bold">{formatPrice(product.price)}</p>
            {soldOut && <p className="text-subtle text-sm">품절된 상품입니다</p>}
          </div>

          <AddToCart productId={product.id} soldOut={soldOut} />
        </div>
      </div>

      <RecentlyViewed product={{ id: product.id, name: product.name, price: product.price }} />
    </main>
  );
}
