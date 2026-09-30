import { expect, test } from "@playwright/test";
import { logIn, newAccount, signUpAndVerify } from "./helpers/account";

test("가입하고 메일을 인증하면 로그인할 수 있다", async ({ page }) => {
  const account = newAccount();

  await signUpAndVerify(page, account);
  await expect(page.getByText("이메일 인증이 끝났습니다")).toBeVisible();

  await logIn(page, account);
  await expect(page.getByRole("button", { name: "로그아웃" })).toBeVisible();
});

test("인증하지 않은 계정은 로그인할 수 없다", async ({ page }) => {
  const account = newAccount();

  await page.goto("/signup");
  await page.getByLabel("아이디").fill(account.loginId);
  await page.getByLabel("이메일").fill(account.email);
  await page.getByLabel("비밀번호").fill(account.password);
  await page.getByLabel("이름").fill(account.name);
  await page.getByRole("button", { name: "가입하기" }).click();

  await expect(page.getByText("메일을 확인하세요")).toBeVisible();

  await page.goto("/login");
  await page.getByLabel("아이디").fill(account.loginId);
  await page.getByLabel("비밀번호").fill(account.password);
  await page.getByRole("button", { name: "로그인" }).click();

  await expect(page.getByText("이메일 인증이 필요합니다")).toBeVisible();
});
