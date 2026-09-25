package com.kassa.payment.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.Instant

// 웹훅 원문을 그대로 쌓는다. 처리는 워커가 따로 한다
@Entity
class WebhookInbox(
    eventId: String,
    payload: String,
    headers: String,
    status: InboxStatus,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    // Standard Webhooks 의 webhook-id 헤더. 유니크 제약이 중복 수신을 막는다
    var eventId: String = eventId
        protected set

    var payload: String = payload
        protected set

    var headers: String = headers
        protected set

    @Enumerated(EnumType.STRING)
    var status: InboxStatus = status
        protected set

    var attemptCount: Int = 0
        protected set

    var error: String? = null
        protected set

    @Column(insertable = false, updatable = false)
    var receivedAt: Instant = Instant.now()
        protected set

    var processedAt: Instant? = null
        protected set

    /** 처리를 마쳤다 */
    fun done(now: Instant) {
        status = InboxStatus.DONE
        processedAt = now
        error = null
    }

    /** RECEIVED 로 남겨 다음 주기에 다시 처리한다 */
    fun retryLater(reason: String) {
        attemptCount += 1
        error = reason.take(255)
    }

    /** 재시도를 멈춘다. 워커가 RECEIVED 만 가져간다 */
    fun giveUp(reason: String, now: Instant) {
        status = InboxStatus.FAILED
        processedAt = now
        error = reason.take(255)
    }
}
