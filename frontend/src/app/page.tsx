import { connection } from "next/server";
import { getProducts } from "@/lib/api";
import { ProductBrowser } from "@/components/ProductBrowser";

// 목록은 서버에서 가져온다. 정렬·필터는 클라이언트 컴포넌트가 맡는다
export default async function Home() {
  // 빌드 때 미리 그리지 않는다. 목록은 요청마다 가져온다
  await connection();
  const products = await getProducts();

  return <ProductBrowser products={products} />;
}
