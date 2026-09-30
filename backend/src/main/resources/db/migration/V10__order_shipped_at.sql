-- 배송 처리 시각. 관리자가 PAID 를 SHIPPED 로 옮길 때 찍는다
ALTER TABLE orders ADD COLUMN shipped_at TIMESTAMPTZ;
