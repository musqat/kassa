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
class OrderCancelTest : IntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var orderRepository: OrderRepository
    @Autowired private lateinit var addressRepository: AddressRepository
    @Autowired private lateinit var cartItemRepository: CartItemRepository
    @Autowired private lateinit var productRepository: ProductRepository
    @Autowired private lateinit var categoryRepository: CategoryRepository
    @Autowired private lateinit var emailTokenRepository: EmailTokenRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var passwordEncoder: PasswordEncoder
    @Autowired private lateinit var tokenIssuer: TokenIssuer

    private lateinit var water: Product
    private lateinit var token: String
    private lateinit var otherToken: String
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

        val category = categoryRepository.save(Category("음료"))
        water = productRepository.save(Product(category, "생수", 4_800, stock = 10))

        token = tokenFor("hong01", "a@example.com").also { userId = lastUserId }
        otherToken = tokenFor("kim02", "b@example.com")
    }

    private var lastUserId: Long = 0

    private fun tokenFor(loginId: String, email: String): String {
        val user = userRepository.save(
            User(loginId, email, passwordEncoder.encode("abcd1234")!!, "홍길동")
                .apply { verifyEmail(Instant.now()) },
        )
        lastUserId = user.id!!
        return tokenIssuer.issue(user.id!!, Instant.now()).value
    }

    private fun placeOrder(withToken: String = token, quantity: Int = 2): String {
        mockMvc.post("/api/cart/items") {
            header("Authorization", "Bearer $withToken")
            contentType = MediaType.APPLICATION_JSON
            content = """{"productId":${water.id},"quantity":$quantity}"""
        }

        val body = mockMvc.post("/api/orders") {
            header("Authorization", "Bearer $withToken")
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

    private fun cancel(orderNo: String, withToken: String = token): ResultActionsDsl =
        mockMvc.post("/api/orders/$orderNo/cancel") {
            header("Authorization", "Bearer $withToken")
        }

    private fun reservedStock(): Int = productRepository.findById(water.id!!).get().reservedStock

    @Test
    fun `취소하면 204 이고 상태가 CANCELED 다`() {
        val orderNo = placeOrder()

        cancel(orderNo).andExpect {
            status { isNoContent() }
        }

        val order = orderRepository.findByOrderNoAndUserId(orderNo, userId)!!
        assertThat(order.status).isEqualTo(OrderStatus.CANCELED)
        assertThat(order.closedAt).isNotNull()
    }

    @Test
    fun `취소하면 선점이 풀린다`() {
        val orderNo = placeOrder()
        assertThat(reservedStock()).isEqualTo(2)

        cancel(orderNo)

        assertThat(reservedStock()).isEqualTo(0)
        assertThat(productRepository.findById(water.id!!).get().availableStock()).isEqualTo(10)
    }

    @Test
    fun `이미 취소한 주문은 409 ORDER_003`() {
        val orderNo = placeOrder()
        cancel(orderNo)

        cancel(orderNo).andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("ORDER_003") }
        }

        assertThat(reservedStock()).isEqualTo(0)
    }

    @Test
    fun `남의 주문은 404 ORDER_002`() {
        val othersOrderNo = placeOrder(withToken = otherToken)

        cancel(othersOrderNo).andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("ORDER_002") }
        }

        assertThat(reservedStock()).isEqualTo(2)
    }

    @Test
    fun `없는 주문번호는 404`() {
        cancel("20260924-ZZZZZZZZ").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("ORDER_002") }
        }
    }

    @Test
    fun `토큰 없이 취소하면 401`() {
        val orderNo = placeOrder()

        mockMvc.post("/api/orders/$orderNo/cancel").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `취소한 뒤 같은 장바구니로 다시 주문할 수 있다`() {
        val orderNo = placeOrder()
        cancel(orderNo)

        // 장바구니는 주문해도 비워지지 않는다. 담지 않고 그대로 다시 주문한다
        val again = placeAgain()

        assertThat(again).isNotEqualTo(orderNo)
        assertThat(reservedStock()).isEqualTo(2)
    }

    private fun placeAgain(): String {
        val body = mockMvc.post("/api/orders") {
            header("Authorization", "Bearer " + token)
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
}
