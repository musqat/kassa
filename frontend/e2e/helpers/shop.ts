import { expect, type Page } from "@playwright/test";
import { API_BASE_URL } from "./env";

/** 상품을 담고 주문서를 채워 결제 화면까지 간다. 주문번호를 리턴한다 */
export async function orderFirstProduct(page: Page, productName: string): Promise<string> {
  await page.goto("/");
  await page.getByRole("link", { name: new RegExp(productName) }).click();
  await page.getByRole("button", { name: "장바구니 담기" }).click();

  await page.goto("/cart");
  await page.getByRole("link", { name: "주문하기" }).click();

  await page.getByLabel("받는 사람").fill("홍길동");
  await page.getByLabel("연락처").fill("010-1234-5678");
  await page.getByLabel("우편번호").fill("06236");
  await page.getByLabel("주소", { exact: true }).fill("서울 강남구 테헤란로 1");
  await page.getByLabel("상세 주소").fill("3층");
  await page.getByRole("button", { name: "주문하기" }).click();

  await page.waitForURL(/\/payments\//);

  const orderNo = page.url().split("/payments/")[1];
  expect(orderNo).toBeTruthy();

  return orderNo;
}

/** 결제 화면에서 결과를 고른다 */
export async function chooseOutcome(page: Page, label: string) {
  await page.getByText(label, { exact: true }).click();
}

/** 가짜 게이트웨이 스위치를 되돌린다 */
export async function resetGateway() {
  await fetch(`${API_BASE_URL}/api/admin/fake-gateway/reset`, { method: "POST" });
}
