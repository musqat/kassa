"use client";

import Link from "next/link";
import { useState } from "react";
import { AuthCard } from "@/components/form/AuthCard";
import { Field, SubmitButton } from "@/components/form/Field";

type Props = {
  title: string;
  hint: string;
  // 가입 여부를 알려주지 않으려고 결과를 하나로 둔다
  doneMessage: string;
  submitLabel: string;
  action: (email: string) => Promise<void>;
};

export function EmailRequestForm({ title, hint, doneMessage, submitLabel, action }: Props) {
  const [email, setEmail] = useState("");
  const [done, setDone] = useState(false);
  const [pending, setPending] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setPending(true);
    // 실패해도 같은 안내를 보여준다
    await action(email).catch(() => undefined);
    setPending(false);
    setDone(true);
  }

  return (
    <main className="mx-auto flex w-full max-w-[380px] flex-col gap-6 px-5 py-14">
      <AuthCard title={title}>
        {done ? (
          <p className="text-muted text-sm leading-relaxed">{doneMessage}</p>
        ) : (
          <form className="flex flex-col gap-5" onSubmit={handleSubmit}>
            <Field
              label="이메일"
              type="email"
              value={email}
              onChange={setEmail}
              hint={hint}
              autoComplete="email"
            />
            <SubmitButton disabled={pending}>{submitLabel}</SubmitButton>
          </form>
        )}
      </AuthCard>

      <Link href="/login" className="text-muted px-1 text-xs underline">
        로그인으로
      </Link>
    </main>
  );
}
