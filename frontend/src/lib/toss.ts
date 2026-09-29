import { loadTossPayments, ANONYMOUS } from "@tosspayments/tosspayments-sdk";

// 공개 키다. 서버로 오는 승인 요청은 시크릿 키로 다시 검증한다
const CLIENT_KEY =
  process.env.NEXT_PUBLIC_TOSS_CLIENT_KEY ?? "test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq";

type PaymentWindowInput = {
  orderNo: string;
  amount: number;
  orderName: string;
};

/** 토스 결제창을 연다. 인증이 끝나면 successUrl 로 되돌아온다 */
export async function openPaymentWindow({ orderNo, amount, orderName }: PaymentWindowInput) {
  const tossPayments = await loadTossPayments(CLIENT_KEY);
  const payment = tossPayments.payment({ customerKey: ANONYMOUS });

  await payment.requestPayment({
    method: "CARD",
    amount: { currency: "KRW", value: amount },
    orderId: orderNo,
    orderName,
    successUrl: `${window.location.origin}/payments/success`,
    failUrl: `${window.location.origin}/payments/fail`,
    card: {
      useEscrow: false,
      flowMode: "DEFAULT",
      useCardPoint: false,
      useAppCardOnly: false,
    },
  });
}
