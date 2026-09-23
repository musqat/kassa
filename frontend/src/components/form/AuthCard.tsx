import type { ReactNode } from "react";

// 계정 화면이 같이 쓰는 카드
export function AuthCard({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="border-line flex flex-col gap-7 rounded-[var(--radius-card)] border bg-white px-6 py-8 shadow-[var(--shadow-card)] sm:px-7 sm:py-9">
      <h1 className="text-lg font-bold tracking-[0.08em]">{title}</h1>
      {children}
    </div>
  );
}
