package com.kassa.order

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
import org.springframework.test.web.servlet.post
import java.time.Duration
import java.time.Instant

@AutoConfigureMockMvc
@Import(MutableClockConfig::class)
class OrderExpiryTest : IntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var orderService: OrderService
    @Autowired private lateinit var orderRepository: OrderRepository
    @Autowired private lateinit var addressRepository: AddressRepository
    @Autowired private lateinit var cartItemRepository: CartItemRepository
    @Autowired private lateinit var productRepository: ProductRepository
    @Autowired private lateinit var categoryRepository: CategoryRepository
    @Autowired private lateinit var emailTokenRepository: EmailTokenRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var passwordEncoder: PasswordEncoder
    @Autowired private lateinit var tokenIssuer: TokenIssuer
    @Autowired private lateinit var clock: MutableClock

    private val expiry: Duration = Duration.ofMinutes(30)

    private lateinit var water: Product
    private lateinit var token: String
    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        orderRepository.deleteAll()
        addressRepository.deleteAll()
        cartItemRepository.deleteAll()
        productRepository.deleteAll()
        categoryRepository.deleteAll()
        emailTokenRepository.deleteAll()
        userRepository.deleteAll()
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

        cartItemRepository.deleteAll()
        return Regex("\"orderNo\":\"([^\"]+)\"").find(body)!!.groupValues[1]
    }

    private fun statusOf(orderNo: String): OrderStatus =
        orderRepository.findByOrderNoAndUserId(orderNo, userId)!!.status

    private fun reservedStock(): Int = productRepository.findById(water.id!!).get().reservedStock

    @Test
    fun `29분 지난 주문은 그대로 둔다`() {
        val orderNo = placeOrder()
        clock.advance(Duration.ofMinutes(29))

        val count = orderService.expireOverdue(expiry)

        assertThat(count).isEqualTo(0)
        assertThat(statusOf(orderNo)).isEqualTo(OrderStatus.PENDING)
        assertThat(reservedStock()).isEqualTo(2)
    }

    @Test
    fun `31분 지난 주문은 FAILED 가 되고 선점이 풀린다`() {
        val orderNo = placeOrder()
        clock.advance(Duration.ofMinutes(31))

        val count = orderService.expireOverdue(expiry)

        assertThat(count).isEqualTo(1)
        assertThat(statusOf(orderNo)).isEqualTo(OrderStatus.FAILED)
        assertThat(reservedStock()).isEqualTo(0)

        val order = orderRepository.findByOrderNoAndUserId(orderNo, userId)!!
        assertThat(order.closedAt).isNotNull()
    }

    @Test
    fun `이미 취소된 주문은 건드리지 않는다`() {
        val orderNo = placeOrder()
        mockMvc.post("/api/orders/$orderNo/cancel") {
            header("Authorization", "Bearer $token")
        }
        clock.advance(Duration.ofMinutes(31))

        val count = orderService.expireOverdue(expiry)

        assertThat(count).isEqualTo(0)
        assertThat(statusOf(orderNo)).isEqualTo(OrderStatus.CANCELED)
    }

    @Test
    fun `여러 건도 한 번에 정리한다`() {
        val first = placeOrder(1)
        val second = placeOrder(1)
        val third = placeOrder(1)
        assertThat(reservedStock()).isEqualTo(3)

        clock.advance(Duration.ofMinutes(31))
        val count = orderService.expireOverdue(expiry)

        assertThat(count).isEqualTo(3)
        assertThat(listOf(first, second, third).map { statusOf(it) })
            .containsOnly(OrderStatus.FAILED)
        assertThat(reservedStock()).isEqualTo(0)
    }

    @Test
    fun `정리한 주문을 다시 정리하지 않는다`() {
        placeOrder()
        clock.advance(Duration.ofMinutes(31))
        orderService.expireOverdue(expiry)

        val second = orderService.expireOverdue(expiry)

        assertThat(second).isEqualTo(0)
        assertThat(reservedStock()).isEqualTo(0)
    }

    @Test
    fun `만료 뒤 그 재고로 다시 주문할 수 있다`() {
        placeOrder(10)
        clock.advance(Duration.ofMinutes(31))
        orderService.expireOverdue(expiry)

        val again = placeOrder(10)

        assertThat(statusOf(again)).isEqualTo(OrderStatus.PENDING)
        assertThat(reservedStock()).isEqualTo(10)
    }
}
