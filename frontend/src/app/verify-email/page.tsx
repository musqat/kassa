"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import { AuthCard } from "@/components/form/AuthCard";
import { Field, FormError, SubmitButton } from "@/components/form/Field";
import { ApiError, confirmVerification, resendVerification } from "@/lib/api";

type State = "idle" | "checking" | "done" | "failed";

function VerifyEmail() {
  const token = useSearchParams().get("token");
  const [state, setState] = useState<State>(token ? "checking" : "idle");
  const [error, setError] = useState<string | null>(null);
  const [email, setEmail] = useState("");
  const [resent, setResent] = useState(false);

  useEffect(() => {
    if (token === null) return;

    confirmVerification(token)
      .then(() => setState("done"))
      .catch((e) => {
        setState("failed");
        setError(e instanceof ApiError ? e.message : "인증하지 못했습니다");
      });
  }, [token]);

  async function handleResend(event: React.FormEvent) {
    event.preventDefault();
    await resendVerification(email).catch(() => undefined);
    setResent(true);
  }

  if (state === "checking") {
    return <p className="text-muted text-sm">인증하는 중입니다</p>;
  }

  if (state === "done") {
    return (
      <>
        <p className="text-sm">이메일 인증이 끝났습니다.</p>
        <Link
          href="/login"
          className="bg-ink flex h-11 items-center justify-center rounded-[var(--radius-field)] text-sm text-white"
        >
          로그인하러 가기
        </Link>
      </>
    );
  }

  return (
    <>
      <FormError message={error} />
      {resent ? (
        <p className="text-muted text-sm leading-relaxed">
          가입된 이메일이면 인증 링크를 보냈습니다. 메일함을 확인해 주세요.
        </p>
      ) : (
        <form className="flex flex-col gap-5" onSubmit={handleResend}>
          <Field
            label="이메일"
            type="email"
            value={email}
            onChange={setEmail}
            hint="가입할 때 쓴 주소로 인증 링크를 다시 보냅니다"
            autoComplete="email"
          />
          <SubmitButton>인증 메일 받기</SubmitButton>
        </form>
      )}
    </>
  );
}

export default function VerifyEmailPage() {
  return (
    <main className="mx-auto flex w-full max-w-[380px] flex-col gap-6 px-5 py-14">
      <AuthCard title="이메일 인증">
        <Suspense fallback={<p className="text-muted text-sm">불러오는 중입니다</p>}>
          <VerifyEmail />
        </Suspense>
      </AuthCard>
    </main>
  );
}
