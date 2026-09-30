package com.kassa.admin

import com.kassa.catalog.domain.Category
import com.kassa.catalog.domain.Product
import com.kassa.catalog.domain.ProductStatus
import com.kassa.catalog.repository.CategoryRepository
import com.kassa.catalog.repository.ProductRepository
import com.kassa.cart.domain.CartItem
import com.kassa.cart.repository.CartItemRepository
import com.kassa.common.security.TokenIssuer
import com.kassa.order.domain.Order
import com.kassa.order.domain.OrderItem
import com.kassa.order.domain.ShippingInfo
import com.kassa.order.repository.OrderRepository
import com.kassa.support.IntegrationTest
import com.kassa.user.domain.Role
import com.kassa.user.domain.User
import com.kassa.user.repository.UserRepository
import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

@AutoConfigureMockMvc
class AdminProductControllerTest : IntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var productRepository: ProductRepository
    @Autowired private lateinit var categoryRepository: CategoryRepository
    @Autowired private lateinit var cartItemRepository: CartItemRepository
    @Autowired private lateinit var orderRepository: OrderRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var passwordEncoder: PasswordEncoder
    @Autowired private lateinit var tokenIssuer: TokenIssuer

    private lateinit var category: Category
    private lateinit var water: Product
    private lateinit var adminToken: String
    private lateinit var userToken: String
    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        category = categoryRepository.save(Category("음료"))
        water = productRepository.save(Product(category, "생수", 4_800, stock = 10))

        adminToken = tokenFor("admin01", "admin@kassa.local", Role.ADMIN)
        userToken = tokenFor("hong01", "a@kassa.local", Role.USER)
    }

    private fun tokenFor(loginId: String, email: String, role: Role): String {
        val user = userRepository.save(
            User(loginId, email, passwordEncoder.encode("abcd1234")!!, "홍길동")
                .apply {
                    verifyEmail(Instant.now())
                    if (role == Role.ADMIN) promote()
                },
        )
        userId = user.id!!

        return tokenIssuer.issue(user.id!!, role, Instant.now()).value
    }

    private fun products(token: String = adminToken, status: String? = null): ResultActionsDsl =
        mockMvc.get("/api/admin/products") {
            header("Authorization", "Bearer $token")
            if (status != null) param("status", status)
        }

    private fun create(body: String, token: String = adminToken): ResultActionsDsl =
        mockMvc.post("/api/admin/products") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

    private fun productNow() = productRepository.findById(water.id!!).get()

    @Test
    fun `토큰이 없으면 401`() {
        mockMvc.get("/api/admin/products").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `일반 회원은 403`() {
        products(userToken).andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `관리자는 숨김 상품까지 본다`() {
        productRepository.save(Product(category, "준비 중", 1_000, status = ProductStatus.HIDDEN))

        products().andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
        }

        // 회원용 목록에는 숨김이 빠진다
        mockMvc.get("/api/products").andExpect {
            jsonPath("$.length()") { value(1) }
        }
    }

    @Test
    fun `상태로 거를 수 있다`() {
        productRepository.save(Product(category, "준비 중", 1_000, status = ProductStatus.HIDDEN))

        products(status = "HIDDEN").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].name") { value("준비 중") }
        }
    }

    @Test
    fun `상품을 등록하면 201 이고 목록에 보인다`() {
        create(
            """
            {
              "categoryId": ${category.id},
              "name": "탄산수 500ml",
              "price": 1200,
              "stock": 50
            }
            """.trimIndent(),
        ).andExpect {
            status { isCreated() }
            jsonPath("$.name") { value("탄산수 500ml") }
            jsonPath("$.stock") { value(50) }
            jsonPath("$.status") { value("ON_SALE") }
        }

        assertThat(productRepository.count()).isEqualTo(2)
    }

    @Test
    fun `없는 분류로 등록하면 400`() {
        create(
            """{"categoryId": 9999, "name": "탄산수", "price": 1200, "stock": 10}""",
        ).andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("COMMON_001") }
        }
    }

    @Test
    fun `상품을 수정하면 값이 바뀐다`() {
        mockMvc.patch("/api/admin/products/${water.id}") {
            header("Authorization", "Bearer $adminToken")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "categoryId": ${category.id},
                  "name": "생수 2L 6입",
                  "price": 5200,
                  "thumbnailUrl": "/products/water.jpg"
                }
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.name") { value("생수 2L 6입") }
            jsonPath("$.price") { value(5200) }
        }

        assertThat(productNow().name).isEqualTo("생수 2L 6입")
    }

    @Test
    fun `재고를 고칠 수 있다`() {
        changeStock(30).andExpect {
            status { isOk() }
            jsonPath("$.stock") { value(30) }
        }

        assertThat(productNow().stock).isEqualTo(30)
    }

    @Test
    fun `선점된 수량보다 적게 내리면 400`() {
        val product = productNow()
        product.reserve(4)
        productRepository.save(product)

        changeStock(2).andExpect {
            status { isBadRequest() }
        }

        assertThat(productNow().stock).isEqualTo(10)
    }

    private fun changeStock(stock: Int): ResultActionsDsl =
        mockMvc.patch("/api/admin/products/${water.id}/stock") {
            header("Authorization", "Bearer $adminToken")
            contentType = MediaType.APPLICATION_JSON
            content = """{"stock": $stock}"""
        }

    @Test
    fun `상태를 판매 종료로 바꾸면 회원 목록에서 빠진다`() {
        mockMvc.patch("/api/admin/products/${water.id}/status") {
            header("Authorization", "Bearer $adminToken")
            contentType = MediaType.APPLICATION_JSON
            content = """{"status": "DELETED"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("DELETED") }
        }

        mockMvc.get("/api/products").andExpect {
            jsonPath("$.length()") { value(0) }
        }
    }

    @Test
    fun `참조가 없으면 삭제된다`() {
        mockMvc.delete("/api/admin/products/${water.id}") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isNoContent() }
        }

        assertThat(productRepository.count()).isZero()
    }

    @Test
    fun `장바구니에만 있으면 담긴 항목까지 지운다`() {
        cartItemRepository.save(CartItem(userId, productNow(), 2))

        mockMvc.delete("/api/admin/products/${water.id}") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isNoContent() }
        }

        assertThat(cartItemRepository.count()).isZero()
        assertThat(productRepository.count()).isZero()
    }

    @Test
    fun `주문에 들어간 상품은 409 CATALOG_002`() {
        placeOrderWith(water)

        mockMvc.delete("/api/admin/products/${water.id}") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("CATALOG_002") }
        }

        assertThat(productRepository.count()).isEqualTo(1)
    }

    /** 주문 이력을 하나 만든다. 결제까지 가지 않고 줄만 남긴다 */
    private fun placeOrderWith(product: Product) {
        val shipping = ShippingInfo("홍길동", "enc", "06236", "서울 강남구 테헤란로 1", null)
        val order = Order("20260930-TESTTEST", userId, product.price, 3_000, shipping)
        order.addItem(OrderItem.of(product, 1))

        orderRepository.save(order)
    }
}
