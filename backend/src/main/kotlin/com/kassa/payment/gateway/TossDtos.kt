package com.kassa.payment.gateway

import java.time.OffsetDateTime

// 토스 응답에서 쓰는 필드만 추렸다. 모르는 필드는 버린다

/** 승인·취소·조회가 모두 이 모양으로 돌아온다 */
data class TossPayment(
    val paymentKey: String,
    val orderId: String,
    val totalAmount: Long,
    val balanceAmount: Long,
    val method: String?,
    /** READY, IN_PROGRESS, WAITING_FOR_DEPOSIT, DONE, CANCELED, PARTIAL_CANCELED, ABORTED, EXPIRED */
    val status: String,
    val requestedAt: OffsetDateTime,
    val approvedAt: OffsetDateTime?,
)

/** 실패 응답. 상태 코드와 함께 온다 */
data class TossError(
    val code: String,
    val message: String,
)

/** 거래 조회 응답 한 줄. 결제 한 건이 승인·취소마다 한 줄씩 생긴다 */
data class TossTransaction(
    val paymentKey: String,
    val orderId: String,
    val transactionKey: String,
    val method: String?,
    val status: String,
    val transactionAt: OffsetDateTime,
    val amount: Long,
)

/** 승인 요청 본문 */
data class TossConfirmRequest(
    val paymentKey: String,
    val orderId: String,
    val amount: Long,
)

/** 취소 요청 본문. cancelAmount 를 비우면 전액 취소다 */
data class TossCancelRequest(
    val cancelReason: String,
)
