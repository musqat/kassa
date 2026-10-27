package com.kassa.admin.dto

import java.time.Instant

enum class AttentionReason {
    /** 결과 확인 스케줄러가 끝내지 못한 승인 요청 */
    PAYMENT_UNKNOWN,

    /** 재시도 상한이나 보상 실패로 멈춘 사가 */
    SAGA_STOPPED,
}

// 사람이 볼 주문 한 줄. since 는 그 상태가 된 시각
data class AdminAttentionResponse(
    val orderNo: String,
    val reason: AttentionReason,
    val since: Instant,
)
