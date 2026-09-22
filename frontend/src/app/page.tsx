import { getProducts } from "@/lib/api";
import { ProductBrowser } from "@/components/ProductBrowser";

// 서버 컴포넌트
// fetch 는 브라우저가 아니라 Next 서버에서 나간다.
// 캐시하지 않는 게 기본이라 요청마다 새로 가져온다.
// 정렬·필터는 상호작용이 필요해서 클라이언트 컴포넌트에 넘긴다.
export default async function Home() {
  const products = await getProducts();

  return <ProductBrowser products={products} />;
}
