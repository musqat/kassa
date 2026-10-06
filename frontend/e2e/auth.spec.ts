import { expect, test } from "@playwright/test";
import { logIn, mailLink, newAccount, signUpAndVerify } from "./helpers/account";

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

test("비밀번호를 재설정하면 기존 로그인이 풀리고 새 비밀번호로 로그인한다", async ({ page }) => {
  const account = newAccount();
  await signUpAndVerify(page, account);
  await logIn(page, account);

  // 같은 초에 발급된 토큰은 살아남는다. 로그인과 재설정 사이를 1초 넘게 띄운다
  await page.waitForTimeout(1100);

  await page.goto("/forgot-password");
  await page.getByLabel("이메일").fill(account.email);
  await page.getByRole("button", { name: "재설정 링크 받기" }).click();
  await expect(page.getByText("가입된 이메일이면 재설정 링크를 보냈습니다")).toBeVisible();

  await page.goto(await mailLink(account.email, "reset-password"));
  await page.getByLabel("새 비밀번호").fill("efgh5678");
  await page.getByRole("button", { name: "비밀번호 바꾸기" }).click();
  await expect(page.getByText("비밀번호를 바꿨습니다")).toBeVisible();

  // 재설정 전에 받은 토큰은 더 이상 통하지 않는다
  await page.goto("/me");
  await page.waitForURL("/login");

  await logIn(page, { ...account, password: "efgh5678" });
});
