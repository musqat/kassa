import { expect, test } from "@playwright/test";
import { logIn, newAccount, signUpAndVerify } from "./helpers/account";
import { ADMIN_LOGIN_ID, ADMIN_PASSWORD, API_BASE_URL } from "./helpers/env";

const admin = {
  loginId: ADMIN_LOGIN_ID,
  password: ADMIN_PASSWORD,
  email: "",
  name: "",
};

/** 회원이 보는 상품 목록에 그 이름이 있는지 */
async function inPublicList(name: string): Promise<boolean> {
  const products: { name: string }[] = await fetch(`${API_BASE_URL}/api/products`).then((res) =>
    res.json(),
  );
  return products.some((p) => p.name === name);
}

test("관리자가 등록한 상품은 회원 목록에 보이고, 판매 종료하면 빠진다", async ({ page }) => {
  const name = `E2E 상품 ${Math.random().toString(36).slice(2, 7)}`;

  await logIn(page, admin);
  await page.goto("/admin");

  const form = page.locator("form");
  await form.getByPlaceholder("상품 이름").fill(name);
  await form.getByPlaceholder("가격").fill("1500");
  await form.getByPlaceholder("재고").fill("20");
  await form.getByRole("button", { name: "등록" }).click();

  const row = page.locator("li", { hasText: name });
  await expect(row).toBeVisible();
  expect(await inPublicList(name)).toBe(true);

  // 판매 종료는 되돌릴 수 없어 확인 창이 뜬다
  page.once("dialog", (dialog) => dialog.accept());
  await row.locator("select").selectOption("DELETED");

  await expect(row.locator("select")).toHaveValue("DELETED");
  expect(await inPublicList(name)).toBe(false);
});

test("로그인하지 않으면 관리자 화면에서 로그인으로 보낸다", async ({ page }) => {
  await page.goto("/admin");
  await page.waitForURL("/login");
});

test("일반 회원은 관리자 화면에서 홈으로 보낸다", async ({ page }) => {
  const account = newAccount();
  await signUpAndVerify(page, account);
  await logIn(page, account);

  await page.goto("/admin");
  await page.waitForURL("/");
  await expect(page.getByRole("link", { name: "관리자" })).toHaveCount(0);
});
