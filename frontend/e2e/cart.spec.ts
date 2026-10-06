import { expect, test, type Page } from "@playwright/test";
import { logIn, newAccount, signUpAndVerify } from "./helpers/account";
import { changeStock, createProduct, endSale } from "./helpers/admin";

test.beforeEach(async ({ page }) => {
  const account = newAccount();
  await signUpAndVerify(page, account);
  await logIn(page, account);
});

async function addToCart(page: Page, name: string, quantity: number) {
  await page.goto("/");
  await page.getByRole("link", { name: new RegExp(name) }).click();
  for (let n = 1; n < quantity; n += 1) {
    await page.getByRole("button", { name: "+" }).click();
  }
  await page.getByRole("button", { name: "장바구니 담기" }).click();
  await expect(page.getByText("장바구니에 담았습니다")).toBeVisible();
}

test("담은 뒤 재고가 줄면 장바구니가 알리고 주문을 막는다", async ({ page }) => {
  const name = await createProduct(5);
  await addToCart(page, name, 3);

  await changeStock(name, 2);
  await page.goto("/cart");

  await expect(page.getByText("남은 수량이 모자랍니다. 수량을 줄여 주세요")).toBeVisible();
  await expect(page.getByRole("link", { name: "주문하기" })).toHaveAttribute(
    "aria-disabled",
    "true",
  );
});

test("담은 뒤 판매가 끝나면 장바구니에 표시되고 결제에서 빠진다", async ({ page }) => {
  const name = await createProduct(5);
  await addToCart(page, name, 1);

  await endSale(name);
  await page.goto("/cart");

  await expect(page.getByText("판매가 끝난 상품이라 결제에서 빠집니다")).toBeVisible();
});
