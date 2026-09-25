package com.kassa.payment.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.Instant

// 대행사와 주고받은 원문을 남긴다. 정산 감사와 버그 추적에 쓴다
@Entity
class PaymentLog(
    paymentId: Long,
    eventType: String,
    rawPayload: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    var paymentId: Long = paymentId
        protected set

    var eventType: String = eventType
        protected set

    var rawPayload: String = rawPayload
        protected set

    @Column(insertable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set
}
