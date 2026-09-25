package com.kassa.payment.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.Instant

@Entity
class Payment(
    orderId: Long,
    amount: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    var orderId: Long = orderId
        protected set

    var amount: Long = amount
        protected set

    // 대행사가 채번한 거래 식별자. 포트원은 transactionId, 토스는 paymentKey
    var transactionId: String? = null
        protected set

    var method: String? = null
        protected set

    @Enumerated(EnumType.STRING)
    var status: PaymentStatus = PaymentStatus.REQUESTED
        protected set

    var cancelIdempotencyKey: String? = null
        protected set

    var failReason: String? = null
        protected set

    @Column(insertable = false, updatable = false)
    var requestedAt: Instant = Instant.now()
        protected set

    var approvedAt: Instant? = null
        protected set

    var canceledAt: Instant? = null
        protected set

    /** REQUESTED 에서만 온다 */
    fun approve(transactionId: String, method: String?, now: Instant) {
        check(status == PaymentStatus.REQUESTED) { "승인할 수 없는 상태입니다: $status" }

        this.transactionId = transactionId
        this.method = method
        status = PaymentStatus.PAID

        approvedAt = now
    }

    /** REQUESTED 에서만 온다 */
    fun fail(reason: String) {
        check(status == PaymentStatus.REQUESTED) { "실패로 돌릴 수 없는 상태입니다: $status" }

        status = PaymentStatus.FAILED
        failReason = reason
    }

    /** PAID 에서만 온다. 대행사 취소가 끝난 뒤에 부른다 */
    fun cancel(now: Instant) {
        check(status == PaymentStatus.PAID) { "취소할 수 없는 상태입니다: $status" }

        status = PaymentStatus.CANCELED
        canceledAt = now
    }

    /** 취소 멱등키를 잡는다. 이미 있으면 그 값을 쓴다 */
    fun startCancel(key: String): String {
        cancelIdempotencyKey?.let { return it }

        cancelIdempotencyKey = key
        return key
    }
}
