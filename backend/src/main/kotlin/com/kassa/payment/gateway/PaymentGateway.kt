package com.kassa.payment.gateway

import java.time.Instant

// 결제 대행사를 가리는 경계. 구현체는 FakePaymentGateway 와 TossPaymentGateway
interface PaymentGateway {

    /** 결제 금액 사전 등록. 이 개념이 없는 대행사면 아무 일도 안 한다 */
    fun preRegister(orderNo: String, amount: Long)

    /**
     * 승인. 같은 멱등키면 대행사가 같은 요청으로 본다.
     * approvalToken 은 결제창이 돌려준 값이다. 토스는 paymentKey 를 넣는다
     */
    fun confirmPayment(
        orderNo: String,
        amount: Long,
        idempotencyKey: String,
        approvalToken: String? = null,
    ): GatewayPayment

    /** 승인된 결제 전액 취소. 부분 취소는 없다 */
    fun cancelPayment(orderNo: String, amount: Long, idempotencyKey: String): GatewayPayment

    /** 대행사에 저장된 결제 상태 조회 */
    fun getPayment(orderNo: String): GatewayPayment?

    /** 웹훅 서명 검증. 통과하면 event_id, 실패하면 null */
    fun verifyWebhook(headers: Map<String, String>, rawBody: String): String?

    /** 기간 안의 결제 목록 조회. 정산 대사용 */
    fun findPayments(from: Instant, to: Instant): List<GatewayPayment>
}

// 대행사마다 다른 응답을 이 모양으로 맞춘다
data class GatewayPayment(
    val orderNo: String,
    val transactionId: String,
    val amount: Long,
    val method: String?,
    val status: GatewayStatus,
    val approvedAt: Instant?,
)

enum class GatewayStatus {
    /** 승인 전 */
    READY,

    /** 승인됨 */
    PAID,

    /** 승인 실패 */
    FAILED,

    /** 취소됨 */
    CANCELED,
}

/** 대행사 요청 거절. 승인 안 됨 */
class GatewayException(message: String, val code: String? = null) : RuntimeException(message)

/** 응답 없음. 승인 여부 모름 */
class GatewayTimeoutException(message: String) : RuntimeException(message)

/** 같은 주문에 승인된 결제가 이미 있음. payment 에 그 결제가 담긴다 */
class AlreadyPaidException(val payment: GatewayPayment) : RuntimeException("이미 승인된 결제입니다")
