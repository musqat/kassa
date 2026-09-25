package com.kassa.payment.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.Instant

// 주문 하나에 사가 하나. order_no 가 유니크다
@Entity
class SagaInstance(
    sagaType: String,
    orderNo: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    var sagaType: String = sagaType
        protected set

    var orderNo: String = orderNo
        protected set

    @Enumerated(EnumType.STRING)
    var status: SagaStatus = SagaStatus.RUNNING
        protected set

    var currentStep: String? = null
        protected set

    @Column(insertable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    var updatedAt: Instant = Instant.now()
        protected set

    /** 복구 스케줄러가 updatedAt 으로 멈춘 사가를 고른다 */
    fun enterStep(stepName: String, now: Instant) {
        currentStep = stepName
        updatedAt = now
    }

    /** RUNNING 에서만 온다 */
    fun complete(now: Instant) {
        check(status == SagaStatus.RUNNING) { "완료할 수 없는 상태입니다: $status" }

        status = SagaStatus.COMPLETED
        updatedAt = now
    }

    /** RUNNING 에서만 온다 */
    fun startCompensating(now: Instant) {
        check(status == SagaStatus.RUNNING) { "되돌릴 수 없는 상태입니다: $status" }

        status = SagaStatus.COMPENSATING
        updatedAt = now
    }

    /** COMPENSATING 에서만 온다 */
    fun compensated(now: Instant) {
        check(status == SagaStatus.COMPENSATING) { "되돌리기 중이 아닙니다: $status" }

        status = SagaStatus.FAILED
        updatedAt = now
    }

    /** COMPENSATING 에서만 온다. 재시도를 멈춘다 */
    fun needsAttention(now: Instant) {
        check(status == SagaStatus.COMPENSATING) { "되돌리기 중이 아닙니다: $status" }

        status = SagaStatus.NEEDS_ATTENTION
        updatedAt = now
    }
}
