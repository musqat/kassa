package com.kassa.payment.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.Instant

// 단계마다 한 행. 어디까지 갔는지는 이 표를 보고 판단한다
@Entity
class SagaStep(
    sagaInstanceId: Long,
    stepName: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    var sagaInstanceId: Long = sagaInstanceId
        protected set

    var stepName: String = stepName
        protected set

    @Enumerated(EnumType.STRING)
    var status: StepStatus = StepStatus.PENDING
        protected set

    var attemptCount: Int = 0
        protected set

    var idempotencyKey: String? = null
        protected set

    // payment_id 만 담는다. 주소·전화번호는 넣지 않는다
    var payload: String? = null
        protected set

    var error: String? = null
        protected set

    var executedAt: Instant? = null
        protected set

    /** 재시도도 여기를 지난다. status 는 건드리지 않는다 */
    fun begin(now: Instant) {
        attemptCount += 1
        executedAt = now
    }

    /** 멱등키를 잡는다. 이미 있으면 그 값을 쓴다 */
    fun claimKey(key: String): String {
        idempotencyKey?.let { return it }

        idempotencyKey = key
        return key
    }

    /** 재시도로 성공했으면 앞선 실패 메시지를 지운다 */
    fun done(payload: String?) {
        status = StepStatus.DONE
        this.payload = payload
        error = null
    }

    /** error 컬럼이 255자다 */
    fun fail(reason: String) {
        status = StepStatus.FAILED
        error = reason.take(255)
    }

    /** DONE 에서만 온다 */
    fun compensated() {
        check(status == StepStatus.DONE) { "되돌릴 수 없는 상태입니다: $status" }

        status = StepStatus.COMPENSATED
    }
}
