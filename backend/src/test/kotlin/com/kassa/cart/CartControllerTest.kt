package com.kassa.cart

import com.kassa.cart.repository.CartItemRepository
import com.kassa.catalog.domain.Category
import com.kassa.catalog.domain.Product
import com.kassa.catalog.domain.ProductStatus
import com.kassa.catalog.repository.CategoryRepository
import com.kassa.catalog.repository.ProductRepository
import com.kassa.common.security.TokenIssuer
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.Instant

@AutoConfigureMockMvc
class CartControllerTest : IntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var cartItemRepository: CartItemRepository

    @Autowired
    private lateinit var emailTokenRepository: EmailTokenRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var tokenIssuer: TokenIssuer

    private lateinit var water: Product
    private lateinit var kettle: Product
    private lateinit var soldOut: Product
    private lateinit var hidden: Product

    private lateinit var token: String
    private lateinit var otherToken: String
    private var otherUserId: Long = 0

    @BeforeEach
    fun setUp() {
        cartItemRepository.deleteAll()
        productRepository.deleteAll()
        categoryRepository.deleteAll()
        emailTokenRepository.deleteAll()
        userRepository.deleteAll()

        val category = categoryRepository.save(Category("음료"))
        water = productRepository.save(Product(category, "생수", 4_800))
        kettle = productRepository.save(Product(category, "주전자", 45_000))
        soldOut = productRepository.save(Product(category, "보리차", 8_900, status = ProductStatus.SOLD_OUT))
        hidden = productRepository.save(Product(category, "준비 중", 9_900, status = ProductStatus.HIDDEN))

        token = tokenFor("hong01", "a@example.com")
        otherToken = tokenFor("kim02", "b@example.com").also { otherUserId = lastUserId }
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

    private fun getCart(withToken: String? = token): ResultActionsDsl = mockMvc.get("/api/cart") {
        if (withToken != null) header("Authorization", "Bearer $withToken")
    }

    private fun addItem(productId: Long, quantity: Int = 1, withToken: String = token): ResultActionsDsl =
        mockMvc.post("/api/cart/items") {
            header("Authorization", "Bearer $withToken")
            contentType = MediaType.APPLICATION_JSON
            content = """{"productId":$productId,"quantity":$quantity}"""
        }

    private fun changeQuantity(itemId: Long, quantity: Int, withToken: String = token): ResultActionsDsl =
        mockMvc.patch("/api/cart/items/$itemId") {
            header("Authorization", "Bearer $withToken")
            contentType = MediaType.APPLICATION_JSON
            content = """{"quantity":$quantity}"""
        }

    private fun removeItem(itemId: Long, withToken: String = token): ResultActionsDsl =
        mockMvc.delete("/api/cart/items/$itemId") {
            header("Authorization", "Bearer $withToken")
        }

    // 담긴 항목의 id. 화면이 쓰는 itemId 와 같다
    private fun itemIdOf(product: Product, userId: Long? = null): Long {
        val items = cartItemRepository.findAll()
        return items.first { it.product.id == product.id && (userId == null || it.userId == userId) }.id!!
    }

    @Test
    fun `토큰 없이 조회하면 401`() {
        getCart(withToken = null).andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("AUTH_001") }
        }
    }

    @Test
    fun `빈 장바구니는 배송비가 0 이다`() {
        getCart().andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(0) }
            jsonPath("$.itemAmount") { value(0) }
            jsonPath("$.shippingFee") { value(0) }
            jsonPath("$.totalAmount") { value(0) }
        }
    }

    @Test
    fun `담으면 201 이고 조회에 한 줄이 나온다`() {
        addItem(water.id!!, quantity = 2).andExpect {
            status { isCreated() }
        }

        getCart().andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(1) }
            jsonPath("$.items[0].name") { value("생수") }
            jsonPath("$.items[0].quantity") { value(2) }
            jsonPath("$.items[0].lineAmount") { value(9_600) }
            jsonPath("$.items[0].orderable") { value(true) }
            jsonPath("$.itemAmount") { value(9_600) }
            jsonPath("$.shippingFee") { value(3_000) }
            jsonPath("$.totalAmount") { value(12_600) }
            jsonPath("$.freeShippingRemaining") { value(20_400) }
        }
    }

    @Test
    fun `같은 상품을 다시 담으면 수량을 더한다`() {
        addItem(water.id!!, quantity = 2)
        addItem(water.id!!, quantity = 3)

        getCart().andExpect {
            jsonPath("$.items.length()") { value(1) }
            jsonPath("$.items[0].quantity") { value(5) }
        }
    }

    @Test
    fun `수량 합이 99 를 넘으면 400 CART_002`() {
        addItem(water.id!!, quantity = 60)

        addItem(water.id!!, quantity = 40).andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("CART_002") }
        }
    }

    @Test
    fun `한 번에 100 개를 담으면 400 COMMON_001`() {
        addItem(water.id!!, quantity = 100).andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("COMMON_001") }
        }
    }

    @Test
    fun `품절 상품은 409 CART_003`() {
        addItem(soldOut.id!!).andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("CART_003") }
        }
    }

    @Test
    fun `숨김 상품은 404 CATALOG_001`() {
        addItem(hidden.id!!).andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("CATALOG_001") }
        }
    }

    @Test
    fun `없는 상품도 404 CATALOG_001`() {
        addItem(99_999).andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("CATALOG_001") }
        }
    }

    @Test
    fun `수량을 바꾸면 금액도 바뀐다`() {
        addItem(water.id!!, quantity = 2)

        changeQuantity(itemIdOf(water), 5).andExpect {
            status { isNoContent() }
        }

        getCart().andExpect {
            jsonPath("$.items[0].quantity") { value(5) }
            jsonPath("$.itemAmount") { value(24_000) }
        }
    }

    @Test
    fun `삭제하면 조회에서 빠진다`() {
        addItem(water.id!!)

        removeItem(itemIdOf(water)).andExpect {
            status { isNoContent() }
        }

        getCart().andExpect {
            jsonPath("$.items.length()") { value(0) }
        }
    }

    @Test
    fun `남의 항목은 수정도 삭제도 404 CART_001`() {
        addItem(water.id!!, withToken = otherToken)
        val othersItemId = itemIdOf(water, otherUserId)

        changeQuantity(othersItemId, 5).andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("CART_001") }
        }

        removeItem(othersItemId).andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("CART_001") }
        }
    }

    @Test
    fun `담아둔 뒤 품절되면 그 줄은 합계에서 빠진다`() {
        addItem(water.id!!, quantity = 2)
        addItem(kettle.id!!)
        productRepository.save(
            productRepository.findById(kettle.id!!).get().apply { changeStatus(ProductStatus.SOLD_OUT) },
        )

        getCart().andExpect {
            jsonPath("$.items.length()") { value(2) }
            jsonPath("$.items[?(@.name == '주전자')].orderable") { value(false) }
            jsonPath("$.itemAmount") { value(9_600) }
            jsonPath("$.shippingFee") { value(3_000) }
        }
    }

    @Test
    fun `담아둔 뒤 숨김이 되면 목록에서 빠진다`() {
        addItem(water.id!!)
        addItem(kettle.id!!)
        productRepository.save(
            productRepository.findById(kettle.id!!).get().apply { changeStatus(ProductStatus.HIDDEN) },
        )

        getCart().andExpect {
            jsonPath("$.items.length()") { value(1) }
            jsonPath("$.items[0].name") { value("생수") }
        }
    }

    @Test
    fun `담아둔 뒤 가격이 바뀌면 바뀐 가격으로 보인다`() {
        addItem(water.id!!, quantity = 2)
        productRepository.save(
            productRepository.findById(water.id!!).get().apply { changePrice(5_000) },
        )

        getCart().andExpect {
            jsonPath("$.items[0].price") { value(5_000) }
            jsonPath("$.items[0].lineAmount") { value(10_000) }
            jsonPath("$.itemAmount") { value(10_000) }
        }
    }

    @Test
    fun `3만원을 넘으면 배송비가 0 이다`() {
        addItem(kettle.id!!)

        getCart().andExpect {
            jsonPath("$.itemAmount") { value(45_000) }
            jsonPath("$.shippingFee") { value(0) }
            jsonPath("$.totalAmount") { value(45_000) }
            jsonPath("$.freeShippingRemaining") { value(0) }
        }
    }
}
