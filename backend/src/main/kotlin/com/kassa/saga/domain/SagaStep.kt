package com.kassa.saga.domain

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.Instant

// 단계 하나의 실행 기록. 시도 횟수와 멱등키, 결과가 남는다
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

    // payment_id 만. 주소·전화번호는 넣지 않는다
    var payload: String? = null
        protected set

    var error: String? = null
        protected set

    var executedAt: Instant? = null
        protected set

    /** 시도 횟수와 실행 시각 저장. 재시도도 여기를 지난다 */
    fun begin(now: Instant) {
        attemptCount += 1
        executedAt = now
    }

    /** 멱등키 확보. 이미 있으면 그 값 */
    fun claimKey(key: String): String {
        idempotencyKey?.let { return it }

        idempotencyKey = key
        return key
    }

    /** 성공과 payload 저장. 앞선 실패 메시지는 지운다 */
    fun done(payload: String?) {
        status = StepStatus.DONE
        this.payload = payload
        error = null
    }

    /** 실패 사유 저장. error 컬럼이 255자 */
    fun fail(reason: String) {
        status = StepStatus.FAILED
        error = reason.take(255)
    }

    /** 보상 완료 저장. DONE 에서만 */
    fun compensated() {
        check(status == StepStatus.DONE) { "되돌릴 수 없는 상태입니다: $status" }

        status = StepStatus.COMPENSATED
    }
}
