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

test("대행사가 거절하면 주문이 닫히고, 장바구니에서 다시 주문할 수 있다", async ({ page }) => {
  const orderNo = await orderFirstProduct(page, PRODUCT);

  await chooseOutcome(page, "승인 거절");

  await expect(page.getByText("결제가 거절됐습니다")).toBeVisible();

  // 확정 전에 실패해 장바구니는 비지 않았다
  await page.getByRole("link", { name: "장바구니에서 다시 주문하기" }).click();
  await expect(page.getByText(PRODUCT)).toBeVisible();

  await page.goto(`/orders/${orderNo}`);
  await expect(page.getByText("결제 실패")).toBeVisible();
});

test("결제한 주문을 취소하면 취소됨이 된다", async ({ page }) => {
  const orderNo = await orderFirstProduct(page, PRODUCT);
  await chooseOutcome(page, "결제하기");
  await page.waitForURL(`/orders/${orderNo}`);

  await page.getByRole("button", { name: "결제 취소" }).click();

  await expect(page.getByText("취소됨")).toBeVisible();
  await expect(page.getByRole("heading", { name: "결제 취소" })).toBeVisible();
});

test("응답이 없으면 결제 확인 중으로 보이고 취소할 수 없다", async ({ page }) => {
  const orderNo = await orderFirstProduct(page, PRODUCT);

  await chooseOutcome(page, "응답 없음");

  // 대행사에서는 승인됐을 수 있다. 결과가 정해지기 전에는 주문을 닫지 못한다
  await page.waitForURL(`/orders/${orderNo}`);
  await expect(page.getByText("결제 확인 중")).toBeVisible();
  await expect(page.getByRole("button", { name: "주문 취소" })).toHaveCount(0);
  await expect(page.getByRole("link", { name: "결제하기" })).toHaveCount(0);
});

test("금액이 다르면 승인되지 않고 주문이 닫힌다", async ({ page }) => {
  const orderNo = await orderFirstProduct(page, PRODUCT);

  await chooseOutcome(page, "금액 조작");
  await expect(page.getByText("결제 금액이 맞지 않습니다")).toBeVisible();

  await page.goto(`/orders/${orderNo}`);
  await expect(page.getByText("결제 실패")).toBeVisible();
  await expect(page.getByRole("link", { name: "결제하기" })).toHaveCount(0);
});
