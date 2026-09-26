package com.kassa.payment.inbox

enum class InboxStatus {
    /** 처리 전 */
    RECEIVED,

    /** 처리 완료 */
    DONE,

    /** 서명 검증 실패 또는 재시도 초과 */
    FAILED,
}
