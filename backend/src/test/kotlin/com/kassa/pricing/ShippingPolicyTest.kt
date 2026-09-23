package com.kassa.pricing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ShippingPolicyTest {

    @Test
    fun `빈 장바구니에는 배송비가 없다`() {
        assertThat(ShippingPolicy.feeFor(0)).isEqualTo(0)
    }

    @Test
    fun `3만원 미만이면 3천원`() {
        assertThat(ShippingPolicy.feeFor(29_999)).isEqualTo(3_000)
    }

    @Test
    fun `3만원이면 무료다`() {
        assertThat(ShippingPolicy.feeFor(30_000)).isEqualTo(0)
    }

    @Test
    fun `무료배송까지 남은 금액`() {
        assertThat(ShippingPolicy.freeShippingRemaining(9_600)).isEqualTo(20_400)
        assertThat(ShippingPolicy.freeShippingRemaining(30_000)).isEqualTo(0)
        assertThat(ShippingPolicy.freeShippingRemaining(50_000)).isEqualTo(0)
    }
}
