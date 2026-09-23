"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense, useState } from "react";
import { AuthCard } from "@/components/form/AuthCard";
import { Field, FormError, SubmitButton } from "@/components/form/Field";
import { ApiError, resetPassword } from "@/lib/api";

function ResetPassword() {
  const token = useSearchParams().get("token");
  const [newPassword, setNewPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);
  const [done, setDone] = useState(false);

  if (token === null) {
    return (
      <>
        <p className="text-sm leading-relaxed">링크가 올바르지 않습니다.</p>
        <Link href="/forgot-password" className="text-xs underline">
          재설정 링크 다시 받기
        </Link>
      </>
    );
  }

  if (done) {
    return (
      <>
        <p className="text-sm">비밀번호를 바꿨습니다. 기존에 로그인해 둔 기기는 모두 풀립니다.</p>
        <Link
          href="/login"
          className="bg-ink flex h-11 items-center justify-center rounded-[var(--radius-field)] text-sm text-white"
        >
          로그인하러 가기
        </Link>
      </>
    );
  }

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setPending(true);

    try {
      await resetPassword(token!, newPassword);
      setDone(true);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "잠시 뒤 다시 시도해 주세요");
    } finally {
      setPending(false);
    }
  }

  return (
    <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
      <Field
        label="새 비밀번호"
        type="password"
        value={newPassword}
        onChange={setNewPassword}
        hint="8~64자, 영문과 숫자를 모두 포함"
        autoComplete="new-password"
      />
      <FormError message={error} />
      {error !== null && (
        <Link href="/forgot-password" className="text-xs underline">
          재설정 링크 다시 받기
        </Link>
      )}
      <SubmitButton disabled={pending}>비밀번호 바꾸기</SubmitButton>
    </form>
  );
}

export default function ResetPasswordPage() {
  return (
    <main className="mx-auto flex w-full max-w-[380px] flex-col gap-6 px-5 py-14">
      <AuthCard title="비밀번호 재설정">
        <Suspense fallback={<p className="text-muted text-sm">불러오는 중입니다</p>}>
          <ResetPassword />
        </Suspense>
      </AuthCard>
    </main>
  );
}
