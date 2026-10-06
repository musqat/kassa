package com.kassa.saga.domain

// 사가 전체의 상태
// RUNNING ─ 다 끝남 ─▶ COMPLETED
//         ├ 재시도 상한 ─▶ NEEDS_ATTENTION
//         └ 실패 ─▶ COMPENSATING ─ 다 되돌림 ─▶ FAILED
//                              └ 되돌리기 실패 ─▶ NEEDS_ATTENTION
enum class SagaStatus {
    /** 실행 중 */
    RUNNING,

    /** 전부 완료 */
    COMPLETED,

    /** 마친 SagaStep 을 역순으로 되돌리는 중 */
    COMPENSATING,

    /** 보상 완료 */
    FAILED,

    /** 보상 거듭 실패, 또는 재시도 상한. 사람이 본다 */
    NEEDS_ATTENTION,
}

// SagaStep 하나의 상태
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
