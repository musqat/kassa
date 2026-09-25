package com.kassa.payment.domain

enum class SagaStatus {
    /** 단계를 진행하는 중 */
    RUNNING,

    /** 모든 단계가 끝났다 */
    COMPLETED,

    /** 실패해서 끝난 단계를 역순으로 되돌리는 중 */
    COMPENSATING,

    /** 되돌리기까지 끝났다 */
    FAILED,

    /** 되돌리기가 거듭 실패했다. 사람이 봐야 한다 */
    NEEDS_ATTENTION,
}
