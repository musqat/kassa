package com.kassa.pricing

// 장바구니 요약과 주문 금액 계산이 같이 쓴다
object ShippingPolicy {

    const val FREE_THRESHOLD = 30_000L
    const val FEE = 3_000L

    /** 상품 금액에 붙는 배송비 */
    fun feeFor(itemAmount: Long): Long {
        if (itemAmount <= 0) {
            return 0
        }

        if (itemAmount >= FREE_THRESHOLD) {
            return 0
        } else {
            return FEE
        }
    }

    /** 무료배송까지 남은 금액. 이미 넘었으면 0 */
    fun freeShippingRemaining(itemAmount: Long): Long {
        return (FREE_THRESHOLD - itemAmount).coerceAtLeast(0)
    }
}
