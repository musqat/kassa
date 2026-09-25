package com.kassa.payment.domain

enum class PaymentStatus {
    /** 승인을 요청했고 결과를 아직 모른다 */
    REQUESTED,

    /** 대행사가 승인했다 */
    PAID,

    /** 대행사가 승인하지 않았다 */
    FAILED,

    /** 승인된 결제를 되돌렸다 */
    CANCELED,
}

enum class SagaStatus {
    /** 단계를 진행하는 중 */
    RUNNING,

    /** 모든 단계를 마쳤다 */
    COMPLETED,

    /** 마친 단계를 역순으로 되돌리는 중 */
    COMPENSATING,

    /** 되돌리기까지 마쳤다. 결제는 취소됐다 */
    FAILED,

    /** 되돌리기가 거듭 실패해 멈췄다. 사람이 본다 */
    NEEDS_ATTENTION,
}

enum class StepStatus {
    /** 실행 전이거나, 실행했고 결과를 아직 모른다 */
    PENDING,

    /** 이 단계를 마쳤다 */
    DONE,

    /** 이 단계가 실패했다. 재시도 대상이다 */
    FAILED,

    /** 마쳤던 단계를 되돌렸다 */
    COMPENSATED,
}

enum class InboxStatus {
    /** 받아 뒀고 아직 처리하지 않았다 */
    RECEIVED,

    /** 처리를 마쳤다 */
    DONE,

    /** 서명 검증에 실패했거나 처리를 여러 번 실패했다. 다시 처리하지 않는다 */
    FAILED,
}
