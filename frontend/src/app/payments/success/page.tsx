import { Suspense } from "react";
import ConfirmPayment from "./ConfirmPayment";

export default function PaymentSuccessPage() {
  return (
    <main className="mx-auto flex w-full max-w-[560px] flex-col items-start gap-4 px-5 py-14">
      <h1 className="text-lg font-bold tracking-[0.08em]">결제 승인</h1>

      <Suspense fallback={<p className="text-muted text-sm">불러오는 중입니다</p>}>
        <ConfirmPayment />
      </Suspense>
    </main>
  );
}
