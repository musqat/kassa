package com.kassa.payment.inbox

import com.kassa.catalog.domain.Category
import com.kassa.catalog.domain.Product
import com.kassa.catalog.repository.CategoryRepository
import com.kassa.catalog.repository.ProductRepository
import com.kassa.common.security.TokenIssuer
import com.kassa.order.domain.OrderStatus
import com.kassa.order.repository.OrderRepository
import com.kassa.payment.gateway.FakePaymentGateway
import com.kassa.support.IntegrationTest
import com.kassa.user.domain.User
import com.kassa.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Instant

@AutoConfigureMockMvc
class InboxWorkerTest : IntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var orderRepository: OrderRepository
    @Autowired private lateinit var productRepository: ProductRepository
    @Autowired private lateinit var categoryRepository: CategoryRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var inboxRepository: WebhookInboxRepository
    @Autowired private lateinit var passwordEncoder: PasswordEncoder
    @Autowired private lateinit var tokenIssuer: TokenIssuer
    @Autowired private lateinit var gateway: FakePaymentGateway
    @Autowired private lateinit var processor: InboxProcessor

    private lateinit var water: Product
    private lateinit var token: String
    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        gateway.reset()

        val category = categoryRepository.save(Category("음료"))
        water = productRepository.save(Product(category, "생수", 4_800, stock = 10))

        val user = userRepository.save(
            User("hong01", "a@example.com", passwordEncoder.encode("abcd1234")!!, "홍길동")
                .apply { verifyEmail(Instant.now()) },
        )
        userId = user.id!!
        token = tokenIssuer.issue(userId, Instant.now()).value
    }

    private fun placeOrder(quantity: Int = 2): String {
        mockMvc.post("/api/cart/items") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"productId":${water.id},"quantity":$quantity}"""
        }

        val body = mockMvc.post("/api/orders") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "receiver": "홍길동",
                  "phone": "010-1234-5678",
                  "zipcode": "06236",
                  "addr1": "서울 강남구 테헤란로 1"
                }
            """.trimIndent()
        }.andReturn().response.contentAsString

        return Regex("\"orderNo\":\"([^\"]+)\"").find(body)!!.groupValues[1]
    }

    private fun confirm(orderNo: String) {
        mockMvc.post("/api/payments/confirm") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"orderNo":"$orderNo"}"""
        }
    }

    /** 대행사가 보낸 웹훅. 서명이 "valid" 면 통과한다 */
    private fun sendWebhook(orderNo: String, eventId: String = "evt-1") {
        mockMvc.post("/api/payments/webhook") {
            header("webhook-id", eventId)
            header("webhook-signature", "valid")
            contentType = MediaType.APPLICATION_JSON
            content = """{"paymentId":"$orderNo","status":"DONE"}"""
        }
    }

    private fun inbox() = inboxRepository.findAll().single()

    @Test
    fun `승인 뒤 온 웹훅은 DONE 이 되고 주문은 그대로다`() {
        val orderNo = placeOrder()
        confirm(orderNo)
        sendWebhook(orderNo)

        val count = processor.processPending()

        assertThat(count).isEqualTo(1)
        assertThat(inbox().status).isEqualTo(InboxStatus.DONE)
        assertThat(orderRepository.findByOrderNo(orderNo)!!.status).isEqualTo(OrderStatus.PAID)
    }

    @Test
    fun `승인보다 먼저 온 웹훅은 RECEIVED 로 남는다`() {
        val orderNo = placeOrder()
        sendWebhook(orderNo)
        processor.processPending()

        assertThat(inbox().status).isEqualTo(InboxStatus.RECEIVED)
        assertThat(inbox().attemptCount).isEqualTo(1)
        assertThat(orderRepository.findByOrderNo(orderNo)!!.status).isEqualTo(OrderStatus.PENDING)

    }

    @Test
    fun `승인이 끝난 뒤 다음 주기에 처리된다`() {
        val orderNo = placeOrder()
        sendWebhook(orderNo)
        processor.processPending()

        confirm(orderNo)

        processor.processPending()

        assertThat(inbox().status).isEqualTo(InboxStatus.DONE)
        assertThat(orderRepository.findByOrderNo(orderNo)!!.status).isEqualTo(OrderStatus.PAID)
    }

    @Test
    fun `재시도 한계를 넘으면 FAILED 가 된다`() {
        val orderNo = placeOrder()
        sendWebhook(orderNo)
        repeat(6) { processor.processPending() }

        assertThat(inbox().status).isEqualTo(InboxStatus.FAILED)
    }

    @Test
    fun `FAILED 로 들어온 웹훅은 가져가지 않는다`() {
        val orderNo = placeOrder()

        mockMvc.post("/api/payments/webhook") {
            header("webhook-id", "evt-1")
            header("webhook-signature", "forged")
            contentType = MediaType.APPLICATION_JSON
            content = """{"paymentId":"$orderNo","status":"DONE"}"""
        }

        val count = processor.processPending()

        assertThat(count).isEqualTo(0)
        assertThat(inbox().status).isEqualTo(InboxStatus.FAILED)
    }

    @Test
    fun `주문번호가 없는 본문은 바로 FAILED 가 된다`() {
        mockMvc.post("/api/payments/webhook") {
            header("webhook-id", "evt-1")
            header("webhook-signature", "forged")
            contentType = MediaType.APPLICATION_JSON
            content = """{"status":"DONE"}"""
        }

        processor.processPending()
        assertThat(inbox().status).isEqualTo(InboxStatus.FAILED)
        assertThat(inbox().attemptCount).isEqualTo(0)

    }
}
