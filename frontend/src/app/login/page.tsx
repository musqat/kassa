"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Field, FormError, SubmitButton } from "@/components/form/Field";
import { ApiError, login } from "@/lib/api";
import { saveToken } from "@/lib/auth";

export default function LoginPage() {
  const router = useRouter();
  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [unverifiedEmail, setUnverifiedEmail] = useState(false);
  const [pending, setPending] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setUnverifiedEmail(false);
    setPending(true);

    try {
      const token = await login(loginId, password);
      saveToken(token.accessToken, token.expiresIn);
      router.push("/");
    } catch (e) {
      if (e instanceof ApiError) {
        // USER_003 이면 재전송 안내를 같이 보여준다
        setUnverifiedEmail(e.code === "USER_003");
        setError(e.message);
      } else {
        setError("잠시 뒤 다시 시도해 주세요");
      }
    } finally {
      setPending(false);
    }
  }

  return (
    <main className="mx-auto flex w-full max-w-[380px] flex-col gap-6 px-6 py-16">
      <div className="border-line flex flex-col gap-7 rounded-[var(--radius-card)] border bg-white px-7 py-9 shadow-[var(--shadow-card)]">
        <h1 className="text-lg font-bold tracking-[0.08em]">로그인</h1>

        <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
          <Field label="아이디" value={loginId} onChange={setLoginId} autoComplete="username" />
          <Field
            label="비밀번호"
            type="password"
            value={password}
            onChange={setPassword}
            autoComplete="current-password"
          />

          <FormError message={error} />
          {unverifiedEmail && (
            <Link href="/verify-email" className="text-xs underline">
              인증 메일 다시 받기
            </Link>
          )}

          <SubmitButton disabled={pending}>로그인</SubmitButton>
        </form>
      </div>

      <div className="text-muted flex justify-between px-1 text-xs">
        <Link href="/signup" className="underline">
          회원가입
        </Link>
        <span className="flex gap-4">
          <Link href="/find-login-id" className="underline">
            아이디 찾기
          </Link>
          <Link href="/forgot-password" className="underline">
            비밀번호 찾기
          </Link>
        </span>
      </div>
    </main>
  );
}
