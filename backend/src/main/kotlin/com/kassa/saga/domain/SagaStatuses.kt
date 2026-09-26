package com.kassa.saga.domain

enum class SagaStatus {
    /** 실행 중 */
    RUNNING,

    /** 전부 완료 */
    COMPLETED,

    /** 역순 보상 중 */
    COMPENSATING,

    /** 보상 완료 */
    FAILED,

    /** 보상 거듭 실패. 사람이 본다 */
    NEEDS_ATTENTION,
}

enum class StepStatus {
    /** 실행 전이거나 진행 중 */
    PENDING,

    /** 완료 */
    DONE,

    /** 실패 */
    FAILED,

    /** 보상 완료 */
    COMPENSATED,
}
