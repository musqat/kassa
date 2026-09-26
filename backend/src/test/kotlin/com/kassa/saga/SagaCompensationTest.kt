package com.kassa.saga

import com.kassa.cart.repository.CartItemRepository
import com.kassa.catalog.domain.Category
import com.kassa.catalog.domain.Product
import com.kassa.catalog.repository.CategoryRepository
import com.kassa.catalog.repository.ProductRepository
import com.kassa.common.security.TokenIssuer
import com.kassa.order.domain.OrderStatus
import com.kassa.order.repository.AddressRepository
import com.kassa.order.repository.OrderRepository
import com.kassa.order.service.OrderService
import com.kassa.payment.domain.PaymentStatus
import com.kassa.payment.gateway.FakePaymentGateway
import com.kassa.payment.repository.PaymentRepository
import com.kassa.saga.domain.SagaStatus
import com.kassa.saga.domain.StepStatus
import com.kassa.saga.repository.SagaInstanceRepository
import com.kassa.saga.repository.SagaStepRepository
import com.kassa.support.IntegrationTest
import com.kassa.support.MutableClock
import com.kassa.support.MutableClockConfig
import com.kassa.user.domain.User
import com.kassa.user.repository.EmailTokenRepository
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
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post
import java.time.Duration
import java.time.Instant

@AutoConfigureMockMvc
@Import(MutableClockConfig::class)
class SagaCompensationTest : IntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var addressRepository: AddressRepository

    @Autowired
    private lateinit var cartItemRepository: CartItemRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var emailTokenRepository: EmailTokenRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var instanceRepository: SagaInstanceRepository

    @Autowired
    private lateinit var stepRepository: SagaStepRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var tokenIssuer: TokenIssuer

    @Autowired
    private lateinit var gateway: FakePaymentGateway

    @Autowired
    private lateinit var recorder: SagaRecorder

    @Autowired
    private lateinit var recoveryService: SagaRecoveryService

    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var clock: MutableClock

    private val expiry: Duration = Duration.ofMinutes(30)

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

    private fun confirm(orderNo: String): ResultActionsDsl =
        mockMvc.post("/api/payments/confirm") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"orderNo":"$orderNo"}"""
        }

    /**
     * 확정 단계를 실패시킨다.
     *
     * 선점을 몰래 풀어 두면 confirmReservation 이 check 에 걸린다.
     * 주문은 2개를 선점한 상태인데 상품에는 0 이 남는다
     */
    private fun breakConfirmStep(quantity: Int = 2) {
        val product = productRepository.findById(water.id!!).get()
        product.releaseReservation(quantity)
        productRepository.save(product)
    }

    private fun productNow() = productRepository.findById(water.id!!).get()

    @Test
    fun `확정이 실패하면 승인을 되돌리고 사가가 FAILED 로 끝난다`() {
        val orderNo = placeOrder()
        breakConfirmStep()

        confirm(orderNo).andExpect { status { is5xxServerError() } }

        val instance = instanceRepository.findByOrderNo(orderNo)!!
        assertThat(instance.status).isEqualTo(SagaStatus.FAILED)

        val steps = stepRepository.findAllBySagaInstanceIdOrderByIdAsc(instance.id!!)
        assertThat(steps.first { it.stepName == "APPROVE_PAYMENT" }.status)
            .isEqualTo(StepStatus.COMPENSATED)
        assertThat(steps.first { it.stepName == "CONFIRM_ORDER" }.status)
            .isEqualTo(StepStatus.FAILED)
    }

    @Test
    fun `확정이 실패하면 결제가 취소된다`() {
        val orderNo = placeOrder()
        breakConfirmStep()
        confirm(orderNo)
        val order = orderRepository.findByOrderNo(orderNo)!!
        val payment = paymentRepository.findAllByOrderIdOrderByIdDesc(order.id!!).single()

        assertThat(payment.status).isEqualTo(PaymentStatus.CANCELED)
        assertThat(payment.canceledAt).isNotNull
    }

    @Test
    fun `취소까지 실패하면 NEEDS_ATTENTION 으로 멈춘다`() {
        gateway.cancelFailCount = 99

        val orderNo = placeOrder()
        breakConfirmStep()
        confirm(orderNo)
        val instance = instanceRepository.findByOrderNo(orderNo)!!
        val order = orderRepository.findByOrderNo(orderNo)!!
        val payment = paymentRepository.findAllByOrderIdOrderByIdDesc(order.id!!).single()
        assertThat(payment.status).isEqualTo(PaymentStatus.PAID)
    }

    @Test
    fun `멈춘 사가를 복구 스케줄러가 이어서 끝낸다`() {
        val orderNo = placeOrder()
        recorder.startOrFind(orderNo)
        clock.advance(Duration.ofMinutes(11))
        val count = recoveryService.recoverStuck(Duration.ZERO)

        assertThat(count).isEqualTo(1)
        assertThat(orderRepository.findByOrderNo(orderNo)!!.status).isEqualTo(OrderStatus.PAID)
    }

    @Test
    fun `사가가 시작된 주문은 만료되지 않는다`() {
        val orderNo = placeOrder()
        recorder.startOrFind(orderNo)
        clock.advance(Duration.ofMinutes(31))
        val count = orderService.expireOverdue(expiry) { instanceRepository.existsByOrderNo(it) }
        assertThat(count).isEqualTo(0)
        assertThat(orderRepository.findByOrderNo(orderNo)!!.status).isEqualTo(OrderStatus.PENDING)
    }

    @Test
    fun `사가가 없는 주문은 그대로 만료된다`() {
        val orderNo = placeOrder()
        clock.advance(Duration.ofMinutes(31))
        val count = orderService.expireOverdue(expiry) { instanceRepository.existsByOrderNo(it) }
        assertThat(count).isEqualTo(1)
        assertThat(orderRepository.findByOrderNo(orderNo)!!.status).isEqualTo(OrderStatus.FAILED)
        assertThat(productNow().reservedStock).isEqualTo(0)
    }
}
