package com.kassa.payment.domain

enum class InboxStatus {
    /** 받아서 쌓아 뒀다. 워커가 가져갈 대상이다 */
    RECEIVED,

    /** 처리했다 */
    DONE,

    /** 서명 검증에 실패했거나, 처리를 여러 번 실패했다 */
    FAILED,
}
