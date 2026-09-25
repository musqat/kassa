package com.kassa.payment

import com.kassa.cart.repository.CartItemRepository
import com.kassa.catalog.domain.Category
import com.kassa.catalog.domain.Product
import com.kassa.catalog.repository.CategoryRepository
import com.kassa.catalog.repository.ProductRepository
import com.kassa.common.security.TokenIssuer
import com.kassa.order.repository.AddressRepository
import com.kassa.order.repository.OrderRepository
import com.kassa.payment.gateway.FakePaymentGateway
import com.kassa.payment.gateway.GatewayException
import com.kassa.support.IntegrationTest
import com.kassa.user.domain.User
import com.kassa.user.repository.EmailTokenRepository
import com.kassa.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
class PreRegisterTest : IntegrationTest() {

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
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var tokenIssuer: TokenIssuer

    @Autowired
    private lateinit var gateway: FakePaymentGateway

    private lateinit var water: Product
    private lateinit var token: String

    @BeforeEach
    fun setUp() {
        orderRepository.deleteAll()
        addressRepository.deleteAll()
        cartItemRepository.deleteAll()
        productRepository.deleteAll()
        categoryRepository.deleteAll()
        emailTokenRepository.deleteAll()
        userRepository.deleteAll()
        gateway.reset()

        val category = categoryRepository.save(Category("음료"))
        water = productRepository.save(Product(category, "생수", 4_800, stock = 10))

        val user = userRepository.save(
            User("hong01", "a@example.com", passwordEncoder.encode("abcd1234")!!, "홍길동")
                .apply { verifyEmail(Instant.now()) },
        )
        token = tokenIssuer.issue(user.id!!, Instant.now()).value
    }

    /** 장바구니에 담고 주문한다. 응답 본문을 돌려준다 */
    private fun placeOrder(quantity: Int = 2): String {
        mockMvc.post("/api/cart/items") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"productId":${water.id},"quantity":$quantity}"""
        }

        return mockMvc.post("/api/orders") {
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
    }

    private fun orderNoOf(body: String): String =
        Regex("\"orderNo\":\"([^\"]+)\"").find(body)!!.groupValues[1]

    @Test
    fun `주문을 만들면 그 금액이 대행사에 등록된다`() {
        val body = placeOrder()
        val orderNo = orderNoOf(body)

        // 등록된 금액은 게이트웨이에 승인을 걸어 보면 알 수 있다
        // 같은 금액으로 승인하면 통과한다
        val approved = gateway.confirmPayment(orderNo, 12_600, "key-1")

        assertThat(approved.amount).isEqualTo(12_600)
        assertThat(body).contains("\"payable\":true")
    }

    @Test
    fun `등록된 금액과 다르면 승인이 거절된다`() {
        var body = placeOrder()
        val orderNo = orderNoOf(body)
        assertThatThrownBy { gateway.confirmPayment(orderNo, 9_999, "key-1") }
            .isInstanceOf(GatewayException::class.java)
    }

    @Test
    fun `사전 등록이 실패해도 주문은 남는다`() {
        gateway.preRegisterFails = true

        val body = placeOrder()

        assertThat(body).contains("\"payable\":false")
        assertThat(orderRepository.count()).isEqualTo(1)
        assertThat(productRepository.findById(water.id!!).get().reservedStock).isEqualTo(2)
    }
}
