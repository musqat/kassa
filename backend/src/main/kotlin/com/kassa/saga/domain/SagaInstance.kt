package com.kassa.saga.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.Instant

// 주문 하나에 사가 하나. order_no 유니크
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

    /** 갱신 시각만 민다. 복구가 집었다는 표시 */
    fun touch(now: Instant) {
        updatedAt = now
    }

    /** 현재 단계와 갱신 시각 저장 */
    fun enterStep(stepName: String, now: Instant) {
        currentStep = stepName
        updatedAt = now
    }

    /** 완료 저장. RUNNING 에서만 */
    fun complete(now: Instant) {
        check(status == SagaStatus.RUNNING) { "완료할 수 없는 상태입니다: $status" }

        status = SagaStatus.COMPLETED
        updatedAt = now
    }

    /** 보상 시작 저장. RUNNING 에서만 */
    fun startCompensating(now: Instant) {
        check(status == SagaStatus.RUNNING) { "되돌릴 수 없는 상태입니다: $status" }

        status = SagaStatus.COMPENSATING
        updatedAt = now
    }

    /** 보상 완료 저장. COMPENSATING 에서만 */
    fun compensated(now: Instant) {
        check(status == SagaStatus.COMPENSATING) { "되돌리기 중이 아닙니다: $status" }

        status = SagaStatus.FAILED
        updatedAt = now
    }

    /** 재시도 상한 저장. 결과를 모르니 되돌리지 않는다. RUNNING 에서만 */
    fun giveUp(now: Instant) {
        check(status == SagaStatus.RUNNING) { "실행 중이 아닙니다: $status" }

        status = SagaStatus.NEEDS_ATTENTION
        updatedAt = now
    }

    /** 재시도 중단 저장. COMPENSATING 에서만 */
    fun needsAttention(now: Instant) {
        check(status == SagaStatus.COMPENSATING) { "되돌리기 중이 아닙니다: $status" }

        status = SagaStatus.NEEDS_ATTENTION
        updatedAt = now
    }
}
