package com.kassa.payment.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant

class PaymentStateTest {

    private val now: Instant = Instant.parse("2026-09-26T10:00:00Z")

    private fun payment() = Payment(orderId = 1, amount = 21_900)

    private fun paid() = payment().apply { approve("tx-1", "CARD", now) }

    @Test
    fun `승인하면 PAID 가 되고 승인 시각이 남는다`() {
        val payment = payment()

        payment.approve("tx-1", "CARD", now)

        assertThat(payment.status).isEqualTo(PaymentStatus.PAID)
        assertThat(payment.transactionId).isEqualTo("tx-1")
        assertThat(payment.approvedAt).isEqualTo(now)
    }

    @Test
    fun `이미 승인된 결제를 다시 승인하면 예외`() {
        assertThatThrownBy { paid().approve("tx-2", "CARD", now) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("PAID")
    }

    @Test
    fun `실패로 돌리면 사유가 남는다`() {
        val payment = payment()
        payment.fail("카드 한도 초과")
        assertThat(payment.status).isEqualTo(PaymentStatus.FAILED)
        assertThat(payment.failReason).isEqualTo("카드 한도 초과")
    }

    @Test
    fun `승인된 결제는 실패로 돌릴 수 없다`() {
        assertThatThrownBy { paid().fail("카드 한도 초과") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("PAID")
    }

    @Test
    fun `승인된 결제를 취소하면 CANCELED 가 되고 취소 시각이 남는다`() {
        val payment = paid()
        payment.cancel(now)

        assertThat(payment.status).isEqualTo(PaymentStatus.CANCELED)
        assertThat(payment.canceledAt).isEqualTo(now)
    }

    @Test
    fun `승인되지 않은 결제는 취소할 수 없다`() {
        val payment = payment()
        assertThatThrownBy { payment.cancel(now) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("REQUESTED")
    }

    @Test
    fun `취소 멱등키는 한 번 설정하면 바뀌지 않는다`() {
        val payment = paid()

        val first = payment.startCancel("key-1")
        val second = payment.startCancel("key-2")

        assertThat(first).isEqualTo("key-1")
        assertThat(second).isEqualTo("key-1")
    }
}
