import type { Page } from "@playwright/test";
import { API_BASE_URL, MAILPIT_BASE_URL } from "./env";

export type Account = {
  loginId: string;
  email: string;
  password: string;
  name: string;
};

// DB 를 비우지 않고 돌린다. 계정마다 난수를 붙여 서로 부딪히지 않게 한다
export function newAccount(): Account {
  const suffix = Math.random().toString(36).slice(2, 8);

  return {
    loginId: `e2e${suffix}`,
    email: `e2e-${suffix}@kassa.local`,
    password: "abcd1234",
    name: "홍길동",
  };
}

/** 가입하고 인증 메일의 링크를 열어 계정을 쓸 수 있는 상태로 만든다 */
export async function signUpAndVerify(page: Page, account: Account) {
  const created = await fetch(`${API_BASE_URL}/api/users`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      loginId: account.loginId,
      email: account.email,
      password: account.password,
      name: account.name,
    }),
  });

  if (!created.ok) {
    throw new Error(`가입 실패 ${created.status} ${await created.text()}`);
  }

  await page.goto(await mailLink(account.email, "verify-email"));

  // 인증은 화면이 뜬 뒤에 요청으로 끝난다. 먼저 로그인하면 아직 인증 전이라 막힌다
  await page.getByText("이메일 인증이 끝났습니다").waitFor();
}

/** Mailpit 에서 그 주소로 온 마지막 메일을 찾아 path 가 든 링크를 꺼낸다 */
export async function mailLink(email: string, path: string): Promise<string> {
  // 메일은 가입 직후 비동기로 들어온다. 몇 번 다시 본다
  for (let attempt = 0; attempt < 20; attempt += 1) {
    const found = await fetch(
      `${MAILPIT_BASE_URL}/api/v1/search?query=${encodeURIComponent(`to:${email}`)}&limit=1`,
    ).then((res) => res.json());

    const id = found.messages?.[0]?.ID;
    if (id) {
      const message = await fetch(`${MAILPIT_BASE_URL}/api/v1/message/${id}`).then((res) =>
        res.json(),
      );
      const body: string = message.HTML || message.Text;
      const link = body.match(new RegExp(`https?://[^"'\\s<>]*${path}[^"'\\s<>]*`))?.[0];

      if (link) return link;
    }

    await new Promise((resolve) => setTimeout(resolve, 500));
  }

  throw new Error(`${email} 로 온 ${path} 메일을 찾지 못했습니다`);
}

export async function logIn(page: Page, account: Account) {
  await page.goto("/login");
  await page.getByLabel("아이디").fill(account.loginId);
  await page.getByLabel("비밀번호").fill(account.password);
  await page.getByRole("button", { name: "로그인" }).click();
  await page.waitForURL("/");
}
