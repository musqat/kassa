package com.kassa.payment.domain

enum class PaymentStatus {
    /** 승인 요청 */
    REQUESTED,

    /** 승인 완료 */
    PAID,

    /** 승인 거절 */
    FAILED,

    /** 취소됨 */
    CANCELED,
}
