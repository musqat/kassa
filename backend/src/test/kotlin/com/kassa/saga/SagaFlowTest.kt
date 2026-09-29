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
import com.kassa.payment.domain.PaymentStatus
import com.kassa.payment.gateway.FakePaymentGateway
import com.kassa.payment.repository.PaymentRepository
import com.kassa.saga.domain.SagaStatus
import com.kassa.saga.domain.StepStatus
import com.kassa.saga.repository.SagaInstanceRepository
import com.kassa.saga.repository.SagaStepRepository
import com.kassa.support.IntegrationTest
import com.kassa.user.domain.User
import com.kassa.user.repository.EmailTokenRepository
import com.kassa.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post
import java.time.Instant

@AutoConfigureMockMvc
class SagaFlowTest : IntegrationTest() {

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

    private lateinit var water: Product
    private lateinit var token: String
    private lateinit var otherToken: String
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

        val other = userRepository.save(
            User("kim02", "b@example.com", passwordEncoder.encode("abcd1234")!!, "김철수")
                .apply { verifyEmail(Instant.now()) },
        )
        otherToken = tokenIssuer.issue(other.id!!, Instant.now()).value
    }

    /** 담고 주문한다. 주문번호를 돌려준다 */
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

    /** 승인 요청. 응답 검사는 호출한 쪽에서 한다 */
    private fun confirm(orderNo: String, bearer: String = token): ResultActionsDsl =
        mockMvc.post("/api/payments/confirm") {
            header("Authorization", "Bearer $bearer")
            contentType = MediaType.APPLICATION_JSON
            content = """{"orderNo":"$orderNo"}"""
        }

    private fun productNow() = productRepository.findById(water.id!!).get()

    @Test
    fun `승인하면 주문이 PAID 가 되고 사가가 COMPLETED 로 끝난다`() {
        val orderNo = placeOrder()

        confirm(orderNo).andExpect { status { isOk() } }

        val order = orderRepository.findByOrderNoAndUserId(orderNo, userId)!!
        assertThat(order.status).isEqualTo(OrderStatus.PAID)
        assertThat(order.paidAt).isNotNull()

        val instance = instanceRepository.findByOrderNo(orderNo)!!
        assertThat(instance.status).isEqualTo(SagaStatus.COMPLETED)

        val steps = stepRepository.findAllBySagaInstanceIdOrderByIdAsc(instance.id!!)
        assertThat(steps).hasSize(2)
        assertThat(steps.map { it.status }).containsOnly(StepStatus.DONE)
    }

    @Test
    fun `승인하면 재고가 확정 차감되고 장바구니가 비워진다`() {
        val orderNo = placeOrder()

        assertThat(productNow().stock).isEqualTo(10)
        assertThat(productNow().reservedStock).isEqualTo(2)

        confirm(orderNo).andExpect { status { isOk() } }

        assertThat(productNow().stock).isEqualTo(8)
        assertThat(productNow().reservedStock).isEqualTo(0)

        assertThat(cartItemRepository.findAllByUserId(userId)).isEmpty()
    }

    @Test
    fun `승인하면 결제에 거래 식별자와 PAID 가 남는다`() {
        val orderNo = placeOrder()
        confirm(orderNo).andExpect { status { isOk() } }
        val order = orderRepository.findByOrderNoAndUserId(orderNo, userId)!!
        val payments = paymentRepository.findAllByOrderIdOrderByIdDesc(order.id!!)

        assertThat(payments).hasSize(1)

        val payment = payments.single()

        assertThat(payment.status).isEqualTo(PaymentStatus.PAID)
        assertThat(payment.transactionId).isNotNull()
    }

    @Test
    fun `같은 주문을 두 번 승인해도 사가와 결제는 하나다`() {
        val orderNo = placeOrder()
        confirm(orderNo).andExpect { status { isOk() } }
        confirm(orderNo).andExpect { status { isOk() } }

        assertThat(instanceRepository.count()).isEqualTo(1)
        assertThat(paymentRepository.count()).isEqualTo(1)
        assertThat(productNow().reservedStock).isEqualTo(0)
        assertThat(productNow().stock).isEqualTo(8)
    }

    @Test
    fun `남의 주문번호로 승인하면 404`() {
        val orderNo = placeOrder()

        confirm(orderNo, otherToken).andExpect {
            status { isNotFound() }
        }

        val order = orderRepository.findByOrderNoAndUserId(orderNo, userId)!!
        assertThat(order.status).isEqualTo(OrderStatus.PENDING)
    }

    @Test
    fun `대행사가 거절하면 409 PAY_003`() {
        gateway.nextResult = FakePaymentGateway.Outcome.FAIL
        val orderNo = placeOrder()

        confirm(orderNo).andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("PAY_003") }
        }

        val order = orderRepository.findByOrderNoAndUserId(orderNo, userId)!!
        assertThat(order.status).isEqualTo(OrderStatus.PENDING)
    }

    @Test
    fun `응답이 없으면 202 이고 주문은 PENDING 이다`() {
        gateway.nextResult = FakePaymentGateway.Outcome.TIMEOUT
        val orderNo = placeOrder()

        // 실패가 아니라 결과를 모르는 상태다. 확인 스케줄러가 뒤에 정한다
        confirm(orderNo).andExpect {
            status { isAccepted() }
            jsonPath("$.status") { value("PENDING") }
        }

        val instance = instanceRepository.findByOrderNo(orderNo)!!
        assertThat(instance.status).isEqualTo(SagaStatus.RUNNING)
    }

    @Test
    fun `토큰 없이 부르면 401`() {
        mockMvc.post("/api/payments/confirm").andExpect {
            status { isUnauthorized() }

        }
    }
}
