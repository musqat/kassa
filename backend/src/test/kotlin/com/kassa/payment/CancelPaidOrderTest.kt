package com.kassa.payment

import com.kassa.catalog.domain.Category
import com.kassa.catalog.domain.Product
import com.kassa.catalog.repository.CategoryRepository
import com.kassa.catalog.repository.ProductRepository
import com.kassa.common.security.TokenIssuer
import com.kassa.order.domain.OrderStatus
import com.kassa.order.repository.OrderRepository
import com.kassa.payment.domain.PaymentStatus
import com.kassa.payment.gateway.FakePaymentGateway
import com.kassa.payment.repository.PaymentRepository
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
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post
import java.time.Instant

@AutoConfigureMockMvc
class CancelPaidOrderTest : IntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var orderRepository: OrderRepository
    @Autowired private lateinit var productRepository: ProductRepository
    @Autowired private lateinit var categoryRepository: CategoryRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var paymentRepository: PaymentRepository
    @Autowired private lateinit var passwordEncoder: PasswordEncoder
    @Autowired private lateinit var tokenIssuer: TokenIssuer
    @Autowired private lateinit var gateway: FakePaymentGateway

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

    /** 담고 주문하고 승인까지 끝낸다. 주문번호를 리턴한다 */
    private fun paidOrder(quantity: Int = 2): String {
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

        val orderNo = Regex("\"orderNo\":\"([^\"]+)\"").find(body)!!.groupValues[1]

        mockMvc.post("/api/payments/confirm") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"orderNo":"$orderNo"}"""
        }.andExpect { status { isOk() } }

        return orderNo
    }

    private fun cancel(orderNo: String, bearer: String = token): ResultActionsDsl =
        mockMvc.post("/api/orders/$orderNo/cancel") {
            header("Authorization", "Bearer $bearer")
        }

    private fun productNow() = productRepository.findById(water.id!!).get()

    private fun paymentOf(orderNo: String) =
        orderRepository.findByOrderNo(orderNo)!!.let { paymentRepository.findAllByOrderIdOrderByIdDesc(it.id!!).single() }

    @Test
    fun `결제된 주문을 취소하면 주문과 결제가 CANCELED 다`() {
        val orderNo = paidOrder()

        cancel(orderNo).andExpect {
            status { isNoContent() }
        }

        val order = orderRepository.findByOrderNoAndUserId(orderNo, userId)!!
        assertThat(order.status).isEqualTo(OrderStatus.CANCELED)
        assertThat(order.closedAt).isNotNull()

        val payment = paymentOf(orderNo)
        assertThat(payment.status).isEqualTo(PaymentStatus.CANCELED)
        assertThat(payment.canceledAt).isNotNull()
    }

    @Test
    fun `결제된 주문을 취소하면 나갔던 재고가 돌아온다`() {
        val orderNo = paidOrder()
        assertThat(productNow().stock).isEqualTo(8)
        assertThat(productNow().reservedStock).isEqualTo(0)

        cancel(orderNo).andExpect {
            status { isNoContent() }
        }

        // reservedStock 까지 늘리면 아무도 안 잡은 수량이 선점으로 남는다
        assertThat(productNow().stock).isEqualTo(10)
        assertThat(productNow().reservedStock).isEqualTo(0)
    }

    @Test
    fun `대행사 취소가 실패하면 주문도 결제도 그대로다`() {
        val orderNo = paidOrder()
        gateway.cancelFailCount = 1
        cancel(orderNo).andExpect {
            status { isInternalServerError() }
        }

        val order = orderRepository.findByOrderNoAndUserId(orderNo, userId)!!
        assertThat(order.status).isEqualTo(OrderStatus.PAID)
        assertThat(paymentOf(orderNo).status).isEqualTo(PaymentStatus.PAID)
        assertThat(productNow().stock).isEqualTo(8)
    }

    @Test
    fun `취소가 실패한 뒤 다시 부르면 취소된다`() {
        val orderNo = paidOrder()
        gateway.cancelFailCount = 1

        cancel(orderNo).andExpect { status { isInternalServerError() } }
        val firstKey = paymentOf(orderNo).cancelIdempotencyKey

        cancel(orderNo).andExpect { status { isNoContent() } }

        val order = orderRepository.findByOrderNoAndUserId(orderNo, userId)!!
        assertThat(order.status).isEqualTo(OrderStatus.CANCELED)
        assertThat(productNow().stock).isEqualTo(10)

        // 재시도는 같은 키로 보낸다. 키가 바뀌면 대행사가 두 건으로 본다
        assertThat(paymentOf(orderNo).cancelIdempotencyKey).isEqualTo(firstKey)
    }

    @Test
    fun `남의 결제된 주문은 404 ORDER_002`() {
        val orderNo = paidOrder()

        cancel(orderNo, otherToken).andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("ORDER_002") }
        }

        val order = orderRepository.findByOrderNoAndUserId(orderNo, userId)!!
        assertThat(order.status).isEqualTo(OrderStatus.PAID)
        assertThat(productNow().stock).isEqualTo(8)
    }
}
