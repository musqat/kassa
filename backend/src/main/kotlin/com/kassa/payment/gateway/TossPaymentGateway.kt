package com.kassa.payment.gateway

import com.kassa.common.error.ErrorCode
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.ObjectMapper

/** 토스가 거절했다. 코드로 어떤 거절인지 가른다 */
private class TossApiException(
    val code: String,
    val status: Int,
    message: String,
) : RuntimeException(message)

// 토스페이먼츠 구현체. 승인 주체는 서버다. 결제창은 인증까지만 하고 paymentKey 를 준다
@Component
@ConditionalOnProperty(name = ["app.payment.gateway"], havingValue = "toss")
class TossPaymentGateway(
    private val tossRestClient: RestClient,
    @Qualifier("tossTransactionsRestClient") private val transactionsClient: RestClient,
    private val objectMapper: ObjectMapper,
) : PaymentGateway {

    /** 토스에는 금액 사전 등록이 없다. 금액 검증은 승인 요청의 amount 가 맡는다 */
    override fun preRegister(orderNo: String, amount: Long) = Unit

    override fun confirmPayment(
        orderNo: String,
        amount: Long,
        idempotencyKey: String,
        approvalToken: String?,
    ): GatewayPayment {
        // 결제창이 돌려준 paymentKey 가 없으면 승인을 걸 수 없다
        val paymentKey = approvalToken ?: throw GatewayException("paymentKey 없음")
        val request = TossConfirmRequest(paymentKey, orderNo, amount)

        try {
            val payment = post("/v1/payments/confirm", request, idempotencyKey)
            return payment.toGatewayPayment()
        } catch (e: TossApiException) {
            if (e.code == "ALREADY_PROCESSED_PAYMENT") {
                val payment = getPayment(orderNo)
                if (payment != null && payment.status == GatewayStatus.PAID) {
                    throw AlreadyPaidException(payment)
                }
            }
            throw GatewayException(e.message ?: "승인 거절", e.code)
        }
    }

    override fun cancelPayment(orderNo: String, amount: Long, idempotencyKey: String): GatewayPayment {
        val found = getPayment(orderNo) ?: throw GatewayException("취소할 결제가 없습니다")
        val paymentKey = found.transactionId

        try {
            val request = TossCancelRequest("주문 취소")
            val canceled = post("/v1/payments/$paymentKey/cancel", request, idempotencyKey)
            return canceled.toGatewayPayment()
        } catch (e: TossApiException) {
            if (e.code == "ALREADY_CANCELED_PAYMENT") {
                return found
            }
            throw GatewayException(e.message ?: "취소 거절", e.code)
        }
    }

    override fun getPayment(orderNo: String): GatewayPayment? {
        try {
            val payment: TossPayment = get("/v1/payments/orders/$orderNo")
            return payment.toGatewayPayment()
        } catch (e: TossApiException) {
            if (e.status == 404) {
                return null
            }
            throw e
        }
    }

    /** 토스 웹훅에는 서명이 없다. 발신자는 IP 로 확인하고 내용은 조회로 다시 읽는다 */
    override fun verifyWebhook(headers: Map<String, String>, rawBody: String): String? =
        // 이벤트 식별자가 본문에 따로 없다. 같은 결제의 같은 상태를 한 건으로 묶을 값을 만든다
        // 못 꺼내면 null 이다. 인박스가 FAILED 로 넣고 워커가 건너뛴다
        runCatching {
            val body = objectMapper.readTree(rawBody)
            val data = body.get("data")
            val eventType = body.get("eventType").asString()

            "$eventType:" + data.get("paymentKey").asString() + ":" + data.get("status").asString()
        }.getOrNull()

    // 한 결제가 승인·취소마다 한 줄씩 생긴다. 주문번호로 묶어 마지막 거래만 남긴다
    override fun findPayments(from: Instant, to: Instant): List<GatewayPayment> {
        // 거래 조회는 오프셋 없는 서울 시각 문자열을 받는다
        val startDate = LocalDateTime.ofInstant(from, SEOUL).withNano(0).toString()
        val endDate = LocalDateTime.ofInstant(to, SEOUL).withNano(0).toString()

        return transactions(startDate, endDate)
            .groupBy { it.orderId }
            .map { (_, rows) -> rows.maxBy { it.transactionAt }.toGatewayPayment() }
    }

    /** 토스 상태를 우리 상태로 옮긴다 */
    private fun statusOf(tossStatus: String): GatewayStatus {
        val status: GatewayStatus =
            when (tossStatus) {
                "DONE" -> GatewayStatus.PAID
                "CANCELED", "PARTIAL_CANCELED" -> GatewayStatus.CANCELED
                "READY", "IN_PROGRESS", "WAITING_FOR_DEPOSIT" -> GatewayStatus.READY
                "ABORTED", "EXPIRED" -> GatewayStatus.FAILED
                else -> throw IllegalStateException("모르는 토스 결제 상태입니다: $tossStatus")
            }
        return status
    }

    private fun TossPayment.toGatewayPayment() = GatewayPayment(
        orderNo = orderId,
        transactionId = paymentKey,
        amount = totalAmount,
        method = method,
        status = statusOf(status),
        approvedAt = approvedAt?.toInstant(),
    )

    private fun TossTransaction.toGatewayPayment() = GatewayPayment(
        orderNo = orderId,
        transactionId = paymentKey,
        amount = amount,
        method = method,
        status = statusOf(status),
        approvedAt = transactionAt.toInstant(),
    )

    /** POST 한 번. 멱등키를 헤더에 싣는다 */
    private fun post(path: String, body: Any, idempotencyKey: String): TossPayment = call {
        tossRestClient.post()
            .uri(path)
            .header(IDEMPOTENCY_KEY, idempotencyKey)
            .body(body)
            .retrieve()
            .body(TossPayment::class.java)!!
    }

    private fun get(path: String): TossPayment = call {
        tossRestClient.get()
            .uri(path)
            .retrieve()
            .body(TossPayment::class.java)!!
    }

    /** 거래 조회. 읽기 제한시간이 긴 클라이언트를 쓴다 */
    private fun transactions(startDate: String, endDate: String): List<TossTransaction> = call {
        transactionsClient.get()
            .uri { builder ->
                builder.path("/v1/transactions")
                    .queryParam("startDate", startDate)
                    .queryParam("endDate", endDate)
                    .build()
            }
            .retrieve()
            .body(Array<TossTransaction>::class.java)!!
            .toList()
    }

    /**
     * 실패를 셋으로 나눈다. 응답이 없으면 GatewayTimeoutException,
     * 토스가 거절하면 TossApiException, 그 밖은 그대로 올린다
     */
    private fun <T> call(block: () -> T): T =
        try {
            block()
        } catch (e: ResourceAccessException) {
            throw GatewayTimeoutException(e.message ?: "토스 응답 없음")
        } catch (e: RestClientResponseException) {
            val error = e.getResponseBodyAs(TossError::class.java)
            throw TossApiException(
                code = error?.code ?: "UNKNOWN",
                status = e.statusCode.value(),
                message = error?.message ?: e.message ?: "토스 요청 실패",
            )
        }

    private companion object {
        const val IDEMPOTENCY_KEY = "Idempotency-Key"
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
