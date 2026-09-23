package com.kassa.order

import com.kassa.cart.repository.CartItemRepository
import com.kassa.catalog.domain.Category
import com.kassa.catalog.domain.Product
import com.kassa.catalog.repository.CategoryRepository
import com.kassa.catalog.repository.ProductRepository
import com.kassa.common.security.TokenIssuer
import com.kassa.order.repository.AddressRepository
import com.kassa.order.repository.OrderRepository
import com.kassa.support.IntegrationTest
import com.kassa.user.domain.User
import com.kassa.user.repository.EmailTokenRepository
import com.kassa.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant

@AutoConfigureMockMvc
class OrderQueryTest : IntegrationTest() {

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
    private lateinit var kettle: Product
    private lateinit var token: String
    private lateinit var otherToken: String

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
        kettle = productRepository.save(Product(category, "주전자", 45_000, stock = 5))

        token = tokenFor("hong01", "a@example.com")
        otherToken = tokenFor("kim02", "b@example.com")
    }

    private fun tokenFor(loginId: String, email: String): String {
        val user = userRepository.save(
            User(loginId, email, passwordEncoder.encode("abcd1234")!!, "홍길동")
                .apply { verifyEmail(Instant.now()) },
        )
        return tokenIssuer.issue(user.id!!, Instant.now()).value
    }

    private fun addToCart(productId: Long, quantity: Int, withToken: String) {
        mockMvc.post("/api/cart/items") {
            header("Authorization", "Bearer $withToken")
            contentType = MediaType.APPLICATION_JSON
            content = """{"productId":$productId,"quantity":$quantity}"""
        }
    }

    private fun place(withToken: String): String {
        val body = mockMvc.post("/api/orders") {
            header("Authorization", "Bearer $withToken")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "receiver": "홍길동",
                  "phone": "010-1234-5678",
                  "zipcode": "06236",
                  "addr1": "서울 강남구 테헤란로 1",
                  "addr2": "10층"
                }
            """.trimIndent()
        }.andReturn().response.contentAsString

        return Regex("\"orderNo\":\"([^\"]+)\"").find(body)!!.groupValues[1]
    }

    private fun orders(withToken: String = token): ResultActionsDsl = mockMvc.get("/api/orders") {
        header("Authorization", "Bearer $withToken")
    }

    private fun order(orderNo: String, withToken: String = token): ResultActionsDsl =
        mockMvc.get("/api/orders/$orderNo") {
            header("Authorization", "Bearer $withToken")
        }

    @Test
    fun `토큰 없이 조회하면 401`() {
        mockMvc.get("/api/orders").andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("AUTH_001") }
        }
    }

    @Test
    fun `주문이 없으면 빈 목록`() {
        orders().andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }
    }

    @Test
    fun `목록은 내 주문만 최신순으로 준다`() {
        addToCart(water.id!!, 1, token)
        val first = place(token)
        cartItemRepository.deleteAll()
        addToCart(kettle.id!!, 1, token)
        val second = place(token)

        addToCart(water.id!!, 1, otherToken)
        place(otherToken)

        orders().andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].orderNo") { value(second) }
            jsonPath("$[1].orderNo") { value(first) }
        }
    }

    @Test
    fun `단건은 주문 상품과 금액을 준다`() {
        addToCart(water.id!!, 2, token)
        val orderNo = place(token)

        order(orderNo).andExpect {
            status { isOk() }
            jsonPath("$.status") { value("PENDING") }
            jsonPath("$.itemAmount") { value(9_600) }
            jsonPath("$.shippingFee") { value(3_000) }
            jsonPath("$.totalAmount") { value(12_600) }
            jsonPath("$.items.length()") { value(1) }
            jsonPath("$.items[0].name") { value("생수") }
            jsonPath("$.items[0].lineAmount") { value(9_600) }
            jsonPath("$.addr1") { value("서울 강남구 테헤란로 1") }
        }
    }

    @Test
    fun `전화번호는 가운데가 가려진다`() {
        addToCart(water.id!!, 1, token)
        val orderNo = place(token)

        order(orderNo).andExpect {
            jsonPath("$.phone") { value("010-****-5678") }
        }
    }

    @Test
    fun `남의 주문번호는 404 ORDER_002`() {
        addToCart(water.id!!, 1, otherToken)
        val othersOrderNo = place(otherToken)

        order(othersOrderNo).andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("ORDER_002") }
        }
    }

    @Test
    fun `없는 주문번호도 404`() {
        order("20260924-ZZZZZZZZ").andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("ORDER_002") }
        }
    }
}
