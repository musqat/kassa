package com.kassa.payment

import com.kassa.catalog.domain.Category
import com.kassa.catalog.domain.Product
import com.kassa.catalog.repository.CategoryRepository
import com.kassa.catalog.repository.ProductRepository
import com.kassa.common.security.TokenIssuer
import com.kassa.order.domain.Order
import com.kassa.order.domain.OrderStatus
import com.kassa.order.repository.OrderRepository
import com.kassa.payment.domain.PaymentStatus
import com.kassa.payment.gateway.FakePaymentGateway
import com.kassa.payment.repository.PaymentRepository
import com.kassa.payment.service.PaymentResultChecker
import com.kassa.saga.SagaRecoveryService
import com.kassa.saga.domain.SagaStatus
import com.kassa.saga.repository.SagaInstanceRepository
import com.kassa.support.IntegrationTest
import com.kassa.support.MutableClock
import com.kassa.support.MutableClockConfig
import com.kassa.user.domain.User
import com.kassa.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.Duration
import java.time.Instant

@AutoConfigureMockMvc
@Import(MutableClockConfig::class)
class PaymentResultCheckerTest : IntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var instanceRepository: SagaInstanceRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var tokenIssuer: TokenIssuer

    @Autowired
    private lateinit var gateway: FakePaymentGateway

    @Autowired
    private lateinit var checker: PaymentResultChecker

    @Autowired
    private lateinit var recoveryService: SagaRecoveryService

    @Autowired
    private lateinit var clock: MutableClock

    private val staleAfter: Duration = Duration.ofMinutes(3)

    private lateinit var water: Product
    private lateinit var token: String
    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        gateway.reset()
        clock.reset()

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

    // 승인 호출이 타임아웃으로 끝난 상태를 만든다. 대행사에는 결제가 남지 않는다
    private fun timeoutOnConfirm(orderNo: String) {
        gateway.nextResult = FakePaymentGateway.Outcome.TIMEOUT
        confirm(orderNo)
        gateway.nextResult = FakePaymentGateway.Outcome.SUCCESS
    }

    private fun paymentOf(orderNo: String) =
        paymentRepository.findAllByOrderIdOrderByIdDesc(orderRepository.findByOrderNo(orderNo)!!.id!!).first()

    @Test
    fun `타임아웃이면 결제가 REQUESTED 로 남고 사가는 RUNNING 이다`() {
        val orderNo = placeOrder()

        timeoutOnConfirm(orderNo)

        assertThat(paymentOf(orderNo).status).isEqualTo(PaymentStatus.REQUESTED)
        assertThat(instanceRepository.findByOrderNo(orderNo)!!.status).isEqualTo(SagaStatus.RUNNING)
        assertThat(orderRepository.findByOrderNo(orderNo)!!.status).isEqualTo(OrderStatus.PENDING)
    }

    @Test
    fun `대행사에 승인 기록이 없으면 FAILED 로 확정된다`() {
        val orderNo = placeOrder()
        timeoutOnConfirm(orderNo)
        clock.advance(Duration.ofMinutes(4))
        val count = checker.checkStale(staleAfter)

        assertThat(count).isEqualTo(1)
        assertThat(paymentOf(orderNo).status).isEqualTo(PaymentStatus.FAILED)
    }

    @Test
    fun `대행사에 승인이 있으면 PAID 로 확정된다`() {
        val orderNo = placeOrder()
        timeoutOnConfirm(orderNo)
        gateway.plant(orderNo, 12_600, Instant.now())
        clock.advance(Duration.ofMinutes(4))
        checker.checkStale(staleAfter)
        assertThat(paymentOf(orderNo).status).isEqualTo(PaymentStatus.PAID)
        assertThat(paymentOf(orderNo).transactionId).isNotNull
    }

    @Test
    fun `3분이 지나지 않은 결제는 가져가지 않는다`() {
        val orderNo = placeOrder()
        timeoutOnConfirm(orderNo)
        checker.checkStale(staleAfter)

        val count = checker.checkStale(staleAfter)

        assertThat(count).isEqualTo(0)
        assertThat(paymentOf(orderNo).status).isEqualTo(PaymentStatus.REQUESTED)
    }

    @Test
    fun `확정된 뒤 복구가 주문까지 끝낸다`() {
        val orderNo = placeOrder()
        timeoutOnConfirm(orderNo)
        gateway.plant(orderNo, 12_600, Instant.now())
        clock.advance(Duration.ofMinutes(11))

        checker.checkStale(staleAfter)
        recoveryService.recoverStuck(Duration.ofMinutes(10))

        assertThat(orderRepository.findByOrderNo(orderNo)!!.status).isEqualTo(OrderStatus.PAID)
        assertThat(instanceRepository.findByOrderNo(orderNo)!!.status).isEqualTo(SagaStatus.COMPLETED)
        assertThat(paymentRepository.count()).isEqualTo(1)
    }
}
