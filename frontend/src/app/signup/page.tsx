"use client";

import Link from "next/link";
import { useState } from "react";
import { AuthCard } from "@/components/form/AuthCard";
import { Field, FormError, SubmitButton } from "@/components/form/Field";
import { ApiError, checkLoginId, resendVerification, signUp } from "@/lib/api";

export default function SignupPage() {
  const [loginId, setLoginId] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [idCheck, setIdCheck] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [sent, setSent] = useState(false);

  async function handleCheckLoginId() {
    setIdCheck(null);
    try {
      const result = await checkLoginId(loginId);
      setIdCheck(result.available ? "쓸 수 있는 아이디입니다" : "이미 쓰는 아이디입니다");
    } catch (e) {
      setIdCheck(e instanceof ApiError ? e.message : "확인하지 못했습니다");
    }
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setPending(true);

    try {
      await signUp({ loginId, email, password, name });
      setSent(true);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "잠시 뒤 다시 시도해 주세요");
    } finally {
      setPending(false);
    }
  }

  if (sent) {
    return (
      <main className="mx-auto flex w-full max-w-[380px] flex-col gap-6 px-5 py-14">
        <AuthCard title="메일을 확인하세요">
          <p className="text-muted text-sm leading-relaxed">
            {email} 으로 인증 링크를 보냈습니다. 링크를 열면 가입이 끝납니다. 메일이 보이지 않으면
            스팸함도 확인해 주세요.
          </p>
          <button
            type="button"
            onClick={() => resendVerification(email)}
            className="border-line-strong h-11 rounded-[var(--radius-field)] border text-sm"
          >
            인증 메일 다시 받기
          </button>
        </AuthCard>
        <Link href="/login" className="text-muted px-1 text-xs underline">
          로그인으로
        </Link>
      </main>
    );
  }

  return (
    <main className="mx-auto flex w-full max-w-[380px] flex-col gap-6 px-5 py-14">
      <AuthCard title="회원가입">
        <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
          <div className="flex items-end gap-2">
            <div className="flex-1">
              <Field
                label="아이디"
                value={loginId}
                onChange={(value) => {
                  setLoginId(value);
                  setIdCheck(null);
                }}
                hint="4~20자, 영문 소문자·숫자·_"
                autoComplete="username"
              />
            </div>
            <button
              type="button"
              onClick={handleCheckLoginId}
              className="border-line-strong mb-[22px] h-11 shrink-0 rounded-[var(--radius-field)] border px-3 text-xs"
            >
              중복확인
            </button>
          </div>
          {idCheck && <p className="text-muted -mt-3 text-xs">{idCheck}</p>}

          <Field
            label="이메일"
            type="email"
            value={email}
            onChange={setEmail}
            autoComplete="email"
          />
          <Field
            label="비밀번호"
            type="password"
            value={password}
            onChange={setPassword}
            hint="8~64자, 영문과 숫자를 모두 포함"
            autoComplete="new-password"
          />
          <Field label="이름" value={name} onChange={setName} autoComplete="name" />

          <FormError message={error} />
          <SubmitButton disabled={pending}>가입하기</SubmitButton>
        </form>
      </AuthCard>

      <Link href="/login" className="text-muted px-1 text-xs underline">
        이미 계정이 있습니다
      </Link>
    </main>
  );
}
