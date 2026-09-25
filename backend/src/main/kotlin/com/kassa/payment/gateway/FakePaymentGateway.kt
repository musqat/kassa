package com.kassa.payment.gateway

import java.time.Clock
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

// 대행사 대신 메모리로 결제를 흉내 낸다. 배포 프로파일에는 올리지 않는다
@Component
@Profile("local", "test")
class FakePaymentGateway(private val clock: Clock) : PaymentGateway {

    private val registered = ConcurrentHashMap<String, Long>()
    private val payments = ConcurrentHashMap<String, GatewayPayment>()

    // 멱등키 → 그때 돌려준 결과. 같은 키로 다시 부르면 이 값을 그대로 준다
    private val byKey = ConcurrentHashMap<String, GatewayPayment>()

    /** 다음 승인을 어떻게 끝낼지 */
    @Volatile
    var nextResult: Outcome = Outcome.SUCCESS

    /** 승인 요청 금액을 이 값으로 바꾼다. 금액 불일치용 */
    @Volatile
    var amountOverride: Long? = null

    /** 남은 취소 실패 횟수 */
    @Volatile
    var cancelFailCount: Int = 0

    /** 금액 사전 등록을 실패시킨다 */
    @Volatile
    var preRegisterFails: Boolean = false

    enum class Outcome {
        SUCCESS,

        /** 승인 안 됨이 확실한 실패 */
        FAIL,

        /** 결과를 모르는 실패 */
        TIMEOUT,
    }

    fun reset() {
        registered.clear()
        payments.clear()
        byKey.clear()
        nextResult = Outcome.SUCCESS
        amountOverride = null
        cancelFailCount = 0
        preRegisterFails = false
    }

    /** 내 DB 를 거치지 않은 결제를 심는다. 정산 대사용 */
    fun plant(orderNo: String, amount: Long, at: Instant) {
        payments[orderNo] = paid(orderNo, amount, at)
    }

    override fun preRegister(orderNo: String, amount: Long) {
        if (preRegisterFails) {
            throw GatewayException("금액을 등록하지 못했습니다", "FAKE_PRE_REGISTER")
        }

        registered[orderNo] = amount
    }

    override fun confirmPayment(orderNo: String, amount: Long, idempotencyKey: String): GatewayPayment {
        // 같은 키로 다시 오면 처리하지 않고 먼저 준 답을 그대로 준다
        byKey[idempotencyKey]?.let { return it }

        val already = payments[orderNo]
        if (already?.status == GatewayStatus.PAID) {
            throw AlreadyPaidException(already)
        }

        when (nextResult) {
            Outcome.TIMEOUT -> throw GatewayTimeoutException("응답이 없습니다")
            Outcome.FAIL -> throw GatewayException("승인이 거절됐습니다", "FAKE_DECLINED")
            Outcome.SUCCESS -> Unit
        }

        val requested = amountOverride ?: amount
        val expected = registered[orderNo]
        if (expected != null && expected != requested) {
            throw GatewayException("등록 금액과 다릅니다: $expected != $requested", "FAKE_AMOUNT")
        }

        val payment = paid(orderNo, requested, Instant.now(clock))
        payments[orderNo] = payment
        byKey[idempotencyKey] = payment
        return payment
    }

    override fun cancelPayment(orderNo: String, amount: Long, idempotencyKey: String): GatewayPayment {
        if (cancelFailCount > 0) {
            cancelFailCount -= 1
            throw GatewayException("취소하지 못했습니다", "FAKE_CANCEL")
        }

        val paid = payments[orderNo]
        if (paid == null || paid.status != GatewayStatus.PAID) {
            throw GatewayException("취소할 결제가 없습니다", "FAKE_NOT_PAID")
        }

        val canceled = paid.copy(status = GatewayStatus.CANCELED)
        payments[orderNo] = canceled
        return canceled
    }

    override fun getPayment(orderNo: String): GatewayPayment? = payments[orderNo]

    // 서명 자리에 "valid" 가 오면 통과로 본다
    override fun verifyWebhook(headers: Map<String, String>, rawBody: String): String? {
        if (headers["webhook-signature"] != "valid") return null

        return headers["webhook-id"]
    }

    override fun findPayments(from: Instant, to: Instant): List<GatewayPayment> =
        payments.values.filter { it.approvedAt != null && it.approvedAt >= from && it.approvedAt < to }

    private fun paid(orderNo: String, amount: Long, at: Instant) = GatewayPayment(
        orderNo = orderNo,
        transactionId = "tx-${UUID.randomUUID()}",
        amount = amount,
        method = "CARD",
        status = GatewayStatus.PAID,
        approvedAt = at,
    )
}
