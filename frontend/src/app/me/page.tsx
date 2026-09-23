"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { Field, FormError, SubmitButton } from "@/components/form/Field";
import { ApiError, changeName, changePassword, getMe, withdraw, type User } from "@/lib/api";
import { clearToken } from "@/lib/auth";

export default function MyPage() {
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    getMe()
      .then(setUser)
      .catch(() => router.replace("/login"));
  }, [router]);

  if (user === null) {
    return (
      <main className="mx-auto w-full max-w-[440px] px-5 py-14">
        <p className="text-muted text-sm">불러오는 중입니다</p>
      </main>
    );
  }

  return (
    <main className="mx-auto flex w-full max-w-[440px] flex-col gap-5 px-5 py-14">
      <section className="border-line flex flex-col gap-3 rounded-[var(--radius-card)] border bg-white px-6 py-7 shadow-[var(--shadow-card)]">
        <h1 className="text-lg font-bold tracking-[0.08em]">내 정보</h1>
        <dl className="divide-line divide-y text-sm">
          <div className="flex justify-between py-3">
            <dt className="text-muted">아이디</dt>
            <dd>{user.loginId}</dd>
          </div>
          <div className="flex justify-between py-3">
            <dt className="text-muted">이메일</dt>
            <dd>{user.email}</dd>
          </div>
        </dl>
      </section>

      <NameSection name={user.name} onChanged={(name) => setUser({ ...user, name })} />
      <PasswordSection />
      <WithdrawSection />
    </main>
  );
}

function NameSection({ name, onChanged }: { name: string; onChanged: (name: string) => void }) {
  const [value, setValue] = useState(name);
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setDone(false);

    try {
      await changeName(value);
      onChanged(value.trim());
      setDone(true);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "바꾸지 못했습니다");
    }
  }

  return (
    <form
      className="border-line flex flex-col gap-4 rounded-[var(--radius-card)] border bg-white px-6 py-7 shadow-[var(--shadow-card)]"
      onSubmit={handleSubmit}
    >
      <h2 className="text-sm font-bold">이름</h2>
      <Field label="이름" value={value} onChange={setValue} autoComplete="name" />
      <FormError message={error} />
      {done && <p className="text-muted text-xs">바꿨습니다</p>}
      <SubmitButton>이름 바꾸기</SubmitButton>
    </form>
  );
}

function PasswordSection() {
  const router = useRouter();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);

    try {
      await changePassword(currentPassword, newPassword);
      // 바꾸는 순간 지금 토큰도 무효가 된다
      clearToken();
      router.replace("/login");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "바꾸지 못했습니다");
    }
  }

  return (
    <form
      className="border-line flex flex-col gap-4 rounded-[var(--radius-card)] border bg-white px-6 py-7 shadow-[var(--shadow-card)]"
      onSubmit={handleSubmit}
    >
      <h2 className="text-sm font-bold">비밀번호</h2>
      <Field
        label="지금 비밀번호"
        type="password"
        value={currentPassword}
        onChange={setCurrentPassword}
        autoComplete="current-password"
      />
      <Field
        label="새 비밀번호"
        type="password"
        value={newPassword}
        onChange={setNewPassword}
        hint="바꾸면 모든 기기에서 로그아웃됩니다"
        autoComplete="new-password"
      />
      <FormError message={error} />
      <SubmitButton>비밀번호 바꾸기</SubmitButton>
    </form>
  );
}

function WithdrawSection() {
  const router = useRouter();
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [asking, setAsking] = useState(false);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);

    try {
      await withdraw(password);
      clearToken();
      router.replace("/");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "탈퇴하지 못했습니다");
    }
  }

  if (!asking) {
    return (
      <button
        type="button"
        onClick={() => setAsking(true)}
        className="text-muted self-start px-1 text-xs underline"
      >
        탈퇴하기
      </button>
    );
  }

  return (
    <form
      className="border-line flex flex-col gap-4 rounded-[var(--radius-card)] border bg-white px-6 py-7 shadow-[var(--shadow-card)]"
      onSubmit={handleSubmit}
    >
      <h2 className="text-sm font-bold">탈퇴</h2>
      <p className="text-muted text-xs leading-relaxed">
        탈퇴하면 장바구니가 비워지고 계정 정보가 지워집니다. 같은 아이디로 다시 가입할 수 있습니다.
      </p>
      <Field
        label="비밀번호"
        type="password"
        value={password}
        onChange={setPassword}
        autoComplete="current-password"
      />
      <FormError message={error} />
      <div className="flex gap-2">
        <button
          type="button"
          onClick={() => setAsking(false)}
          className="border-line-strong h-11 flex-1 rounded-[var(--radius-field)] border text-sm"
        >
          그만두기
        </button>
        <button
          type="submit"
          className="bg-danger h-11 flex-1 rounded-[var(--radius-field)] text-sm text-white"
        >
          탈퇴하기
        </button>
      </div>
    </form>
  );
}
