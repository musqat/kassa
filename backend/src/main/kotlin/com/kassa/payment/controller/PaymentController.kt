package com.kassa.payment.controller

import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import com.kassa.common.security.userId
import com.kassa.order.repository.OrderRepository
import com.kassa.payment.dto.ConfirmPaymentRequest
import com.kassa.payment.dto.ConfirmPaymentResponse
import com.kassa.saga.OrderSagaOrchestrator
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "결제")
@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/api/payments")
class PaymentController(
    private val orchestrator: OrderSagaOrchestrator,
    private val orderRepository: OrderRepository,
) {

    @Operation(
        summary = "결제 승인",
        description = "대행사 승인을 걸고 주문을 확정한다. 남의 주문번호는 404",
    )
    @PostMapping("/confirm")
    fun confirm(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: ConfirmPaymentRequest,
    ): ConfirmPaymentResponse {
        val userId = jwt.userId()
        orchestrator.start(request.orderNo, userId, request.approvalToken)

        // 주문 상태가 진실이다. 사가 기록이 아니라 orders 를 읽어 돌려준다
        val order = orderRepository.findByOrderNoAndUserId(request.orderNo, userId)
            ?: throw BusinessException(ErrorCode.ORDER_NOT_FOUND)

        return ConfirmPaymentResponse(order.orderNo, order.status)
    }
}
