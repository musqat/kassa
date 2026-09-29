package com.kassa.payment.gateway

import java.time.Duration
import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.web.client.RestClient
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

// 실제 토스를 부른다. `gradlew tossTest` 로만 돈다
// 승인은 결제창이 준 paymentKey 가 있어야 해서 브라우저 확인에서 본다
@Tag("toss")
@EnabledIfEnvironmentVariable(named = "TOSS_SECRET_KEY", matches = ".+")
class TossPaymentGatewayTest {

    private val properties = TossProperties(
        baseUrl = "https://api.tosspayments.com",
        secretKey = System.getenv("TOSS_SECRET_KEY"),
        connectTimeout = Duration.ofSeconds(3),
        readTimeout = Duration.ofSeconds(10),
        transactionsTimeout = Duration.ofSeconds(60),
    )

    private val config = TossConfig()
    private val builder: RestClient.Builder = RestClient.builder()

    private val gateway = TossPaymentGateway(
        tossRestClient = config.tossRestClient(builder, properties),
        transactionsClient = config.tossTransactionsRestClient(builder, properties),
        objectMapper = JsonMapper.builder().build(),
    )

    @Test
    fun `없는 주문을 조회하면 null 이다`() {
        assertThat(gateway.getPayment("KASSA-NOT-EXIST-1")).isNull()
    }

    @Test
    fun `승인 토큰이 없으면 GatewayException`() {
        assertThatThrownBy {
            gateway.confirmPayment(
                orderNo = "KASSA-NOT-EXIST-1",
                amount = 1_000,
                idempotencyKey = UUID.randomUUID().toString(),
            )
        }.isInstanceOf(GatewayException::class.java)

    }

    @Test
    fun `없는 paymentKey 로 승인하면 GatewayException`() {
        assertThatThrownBy {
            gateway.confirmPayment(
                orderNo = "KASSA-NOT-EXIST-1",
                amount = 1_000,
                idempotencyKey = UUID.randomUUID().toString(),
                approvalToken = "no-such-payment-key"
            )
        } .hasFieldOrPropertyWithValue("code", "NOT_FOUND_PAYMENT_SESSION")
    }

    @Test
    fun `없는 결제를 취소하면 GatewayException`() {
        assertThatThrownBy {
            gateway.cancelPayment(
                orderNo = "KASSA-NOT-EXIST-1",
                amount = 1_000,
                idempotencyKey = UUID.randomUUID().toString(),
            )
        }.isInstanceOf(GatewayException::class.java)
    }

    @Test
    fun `거래 조회는 주문번호마다 한 건씩 준다`() {
        val to = Instant.now()
        val from = to.minus(Duration.ofDays(1))

        val payments = gateway.findPayments(from, to)

        // 문서 테스트 키는 남의 상점 거래까지 같이 준다. 건수는 볼 수 없고 모양만 본다
        assertThat(payments.map { it.orderNo }).doesNotHaveDuplicates()
    }
}
