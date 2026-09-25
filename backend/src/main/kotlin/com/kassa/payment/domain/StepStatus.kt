package com.kassa.payment.domain

enum class StepStatus {
    /** 실행을 시작했고 결과를 아직 모른다 */
    PENDING,

    /** 끝났다 */
    DONE,

    /** 실패했다 */
    FAILED,

    /** 끝났던 단계를 되돌렸다 */
    COMPENSATED,
}
