package com.kassa.payment.dto

import com.kassa.order.domain.OrderStatus
import jakarta.validation.constraints.NotBlank

data class ConfirmPaymentRequest(
    @field:NotBlank
    val orderNo: String,

    // 결제창이 돌려준 값. 토스는 paymentKey
    val approvalToken: String? = null,
)

data class ConfirmPaymentResponse(
    val orderNo: String,
    val status: OrderStatus,
)
