package com.kassa.payment.gateway

import java.time.Instant

// 결제 대행사를 가리는 경계. 구현체는 FakePaymentGateway 와 PortOnePaymentGateway
interface PaymentGateway {

    /** 금액을 미리 등록한다. 조작된 금액으로 결제창을 열면 대행사가 거절한다 */
    fun preRegister(orderNo: String, amount: Long)

    /** 승인. 같은 멱등키면 대행사가 같은 요청으로 본다 */
    fun confirmPayment(orderNo: String, amount: Long, idempotencyKey: String): GatewayPayment

    /** 전액 취소 */
    fun cancelPayment(orderNo: String, amount: Long, idempotencyKey: String): GatewayPayment

    /** 대행사 쪽 결과. 결과를 모를 때의 근거 */
    fun getPayment(orderNo: String): GatewayPayment?

    /** 서명 검증. 통과하면 event_id 를, 아니면 null */
    fun verifyWebhook(headers: Map<String, String>, rawBody: String): String?

    /** 기간 조회. 정산 대사용 */
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

/** 결과를 아는 실패 */
class GatewayException(message: String, val code: String? = null) : RuntimeException(message)

/** 결과를 모르는 실패. 승인됐는지 알 수 없다 */
class GatewayTimeoutException(message: String) : RuntimeException(message)

/** 이미 승인된 결제. 사가는 성공으로 친다 */
class AlreadyPaidException(val payment: GatewayPayment) : RuntimeException("이미 승인된 결제입니다")
