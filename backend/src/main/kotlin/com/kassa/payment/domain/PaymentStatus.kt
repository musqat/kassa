package com.kassa.payment.domain

enum class PaymentStatus {
    /** 승인을 요청 */
    REQUESTED,

    /** 승인됐다 */
    PAID,

    /** 승인되지 않았다 */
    FAILED,

    /** 승인된 결제를 되돌렸다 */
    CANCELED,
}
