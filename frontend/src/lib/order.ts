import type { OrderStatus } from "@/lib/api";

// 화면에 보여줄 상태 이름
export const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: "결제 대기",
  PAID: "결제 완료",
  SHIPPED: "배송 중",
  FAILED: "결제 실패",
  CANCELED: "취소됨",
};
