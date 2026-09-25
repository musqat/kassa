"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { Field, FormError, SubmitButton } from "@/components/form/Field";
import { ApiError, getCart, placeOrder, type Cart } from "@/lib/api";
import { formatPrice } from "@/lib/format";

export default function CheckoutPage() {
  const router = useRouter();
  const [cart, setCart] = useState<Cart | null>(null);
  const [receiver, setReceiver] = useState("");
  const [phone, setPhone] = useState("");
  const [zipcode, setZipcode] = useState("");
  const [addr1, setAddr1] = useState("");
  const [addr2, setAddr2] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [pending, setPending] = useState(false);

  const load = useCallback(
    () =>
      getCart()
        .then(setCart)
        .catch((e) => {
          if (e instanceof ApiError && e.status === 401) {
            router.replace("/login");
            return;
          }
          setError(e instanceof ApiError ? e.message : "불러오지 못했습니다");
        }),
    [router],
  );

  useEffect(() => {
    const timer = setTimeout(load, 0);
    return () => clearTimeout(timer);
  }, [load]);

  async function handleSubmit(event: React.FormEvent) {
    event.preventDefault();
    setError(null);
    setPending(true);

    try {
      const { orderNo } = await placeOrder({
        receiver,
        phone,
        zipcode,
        addr1,
        addr2,
        saveAddress: true,
      });
      router.push(`/orders/${orderNo}`);
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        router.replace("/login");
        return;
      }
      setError(e instanceof ApiError ? e.message : "주문에 실패했습니다.");
    } finally {
      setPending(false);
    }
  }

  if (cart === null) {
    return (
      <main className="mx-auto w-full max-w-[560px] px-5 py-14">
        <p className="text-muted text-sm">{error ?? "불러오는 중입니다"}</p>
      </main>
    );
  }

  // 결제에서 빠지는 줄은 주문서에도 넣지 않는다
  const orderable = cart.items.filter((item) => item.orderable);

  if (orderable.length === 0) {
    return (
      <main className="mx-auto flex w-full max-w-[560px] flex-col items-start gap-4 px-5 py-14">
        <h1 className="text-lg font-bold tracking-[0.08em]">주문서</h1>
        <p className="text-muted text-sm">주문할 상품이 없습니다</p>
        <Link href="/cart" className="text-xs underline">
          장바구니로
        </Link>
      </main>
    );
  }

  return (
    <main className="mx-auto flex w-full max-w-[560px] flex-col gap-5 px-5 py-14">
      <h1 className="text-lg font-bold tracking-[0.08em]">주문서</h1>

      <section className="border-line flex flex-col gap-3 rounded-[var(--radius-card)] border bg-white px-6 py-6 shadow-[var(--shadow-card)]">
        <h2 className="text-sm font-bold">주문 상품</h2>
        <ul className="flex flex-col gap-2 text-sm">
          {orderable.map((item) => (
            <li key={item.itemId} className="flex justify-between">
              <span>
                {item.name} × {item.quantity}
              </span>
              <span>{formatPrice(item.lineAmount)}</span>
            </li>
          ))}
        </ul>

        <div className="border-line mt-2 flex flex-col gap-2 border-t pt-3 text-sm">
          <div className="flex justify-between">
            <span className="text-muted">상품 금액</span>
            <span>{formatPrice(cart.itemAmount)}</span>
          </div>
          <div className="flex justify-between">
            <span className="text-muted">배송비</span>
            <span>{formatPrice(cart.shippingFee)}</span>
          </div>
          <div className="border-line flex justify-between border-t border-dashed pt-2 font-bold">
            <span>결제 예정 금액</span>
            <span>{formatPrice(cart.totalAmount)}</span>
          </div>
        </div>
      </section>

      <form
        className="border-line flex flex-col gap-4 rounded-[var(--radius-card)] border bg-white px-6 py-6 shadow-[var(--shadow-card)]"
        onSubmit={handleSubmit}
      >
        <h2 className="text-sm font-bold">배송지</h2>
        <Field label="받는 사람" value={receiver} onChange={setReceiver} autoComplete="name" />
        <Field
          label="연락처"
          value={phone}
          onChange={setPhone}
          placeholder="010-1234-5678"
          autoComplete="tel"
        />
        <Field
          label="우편번호"
          value={zipcode}
          onChange={setZipcode}
          placeholder="06236"
          autoComplete="postal-code"
        />
        <Field label="주소" value={addr1} onChange={setAddr1} autoComplete="street-address" />
        <Field label="상세 주소" value={addr2} onChange={setAddr2} />

        <FormError message={error} />
        <SubmitButton disabled={pending}>주문하기</SubmitButton>
      </form>

      <Link href="/cart" className="text-muted px-1 text-xs underline">
        장바구니로 돌아가기
      </Link>
    </main>
  );
}
