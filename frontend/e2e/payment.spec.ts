import { expect, test } from "@playwright/test";
import { logIn, newAccount, signUpAndVerify, type Account } from "./helpers/account";
import { chooseOutcome, orderFirstProduct, resetGateway } from "./helpers/shop";

// 재고가 넉넉한 상품으로 고른다. 머그컵은 재고가 1이라 동시성 테스트용이다
const PRODUCT = "생수 2L 6입";

let account: Account;

test.beforeEach(async ({ page }) => {
  await resetGateway();

  account = newAccount();
  await signUpAndVerify(page, account);
  await logIn(page, account);
});

test("결제하면 주문이 결제 완료가 된다", async ({ page }) => {
  const orderNo = await orderFirstProduct(page, PRODUCT);

  await chooseOutcome(page, "결제하기");

  await page.waitForURL(`/orders/${orderNo}`);
  await expect(page.getByText("결제 완료")).toBeVisible();
  await expect(page.getByText("카드")).toBeVisible();
});

test("대행사가 거절하면 주문이 그대로 남는다", async ({ page }) => {
  const orderNo = await orderFirstProduct(page, PRODUCT);

  await chooseOutcome(page, "승인 거절");

  await expect(page.getByText("결제가 거절됐습니다")).toBeVisible();

  // 거절돼도 주문은 결제 전 상태로 남는다
  await page.goto(`/orders/${orderNo}`);
  await expect(page.getByText("결제 대기")).toBeVisible();
});

test("결제한 주문을 취소하면 취소됨이 된다", async ({ page }) => {
  const orderNo = await orderFirstProduct(page, PRODUCT);
  await chooseOutcome(page, "결제하기");
  await page.waitForURL(`/orders/${orderNo}`);

  await page.getByRole("button", { name: "결제 취소" }).click();

  await expect(page.getByText("취소됨")).toBeVisible();
  await expect(page.getByRole("heading", { name: "결제 취소" })).toBeVisible();
});
