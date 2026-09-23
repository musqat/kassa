package com.kassa.order

import com.kassa.cart.repository.CartItemRepository
import com.kassa.catalog.domain.Category
import com.kassa.catalog.domain.Product
import com.kassa.catalog.domain.ProductStatus
import com.kassa.catalog.repository.CategoryRepository
import com.kassa.catalog.repository.ProductRepository
import com.kassa.common.crypto.PhoneCipher
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
class OrderControllerTest : IntegrationTest() {

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
    @Autowired private lateinit var phoneCipher: PhoneCipher

    private lateinit var water: Product
    private lateinit var kettle: Product
    private lateinit var soldOut: Product
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

        val category = categoryRepository.save(Category("음료"))
        water = productRepository.save(Product(category, "생수", 4_800, stock = 10))
        kettle = productRepository.save(Product(category, "주전자", 45_000, stock = 3))
        soldOut = productRepository.save(Product(category, "보리차", 8_900, status = ProductStatus.SOLD_OUT))

        val user = userRepository.save(
            User("hong01", "a@example.com", passwordEncoder.encode("abcd1234")!!, "홍길동")
                .apply { verifyEmail(Instant.now()) },
        )
        userId = user.id!!
        token = tokenIssuer.issue(userId, Instant.now()).value
    }

    private fun addToCart(productId: Long, quantity: Int) {
        mockMvc.post("/api/cart/items") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"productId":$productId,"quantity":$quantity}"""
        }
    }

    private fun place(withToken: String? = token, saveAddress: Boolean = false): ResultActionsDsl =
        mockMvc.post("/api/orders") {
            if (withToken != null) header("Authorization", "Bearer $withToken")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "receiver": "홍길동",
                  "phone": "010-1234-5678",
                  "zipcode": "06236",
                  "addr1": "서울 강남구 테헤란로 1",
                  "addr2": "10층",
                  "saveAddress": $saveAddress
                }
            """.trimIndent()
        }

    private fun reservedStockOf(product: Product): Int =
        productRepository.findById(product.id!!).get().reservedStock

    @Test
    fun `토큰 없이 주문하면 401`() {
        place(withToken = null).andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("AUTH_001") }
        }
    }

    @Test
    fun `빈 장바구니면 400 ORDER_004`() {
        place().andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("ORDER_004") }
        }
    }

    @Test
    fun `주문하면 201 과 주문번호를 준다`() {
        addToCart(water.id!!, 2)

        place().andExpect {
            status { isCreated() }
            jsonPath("$.totalAmount") { value(12_600) }
        }

        assertThat(orderRepository.findAllByUserIdOrderByIdDesc(userId).first().orderNo)
            .matches("\\d{8}-[23456789A-HJ-NP-Z]{8}")
    }

    @Test
    fun `주문하면 재고가 선점된다`() {
        addToCart(water.id!!, 2)

        place()

        val product = productRepository.findById(water.id!!).get()
        assertThat(product.stock).isEqualTo(10)
        assertThat(product.reservedStock).isEqualTo(2)
        assertThat(product.availableStock()).isEqualTo(8)
    }

    @Test
    fun `주문해도 장바구니는 그대로다`() {
        addToCart(water.id!!, 2)

        place()

        assertThat(cartItemRepository.count()).isEqualTo(1L)
    }

    @Test
    fun `재고보다 많으면 409 ORDER_001 이고 선점이 남지 않는다`() {
        addToCart(kettle.id!!, 3)
        place()
        addToCart(water.id!!, 1)

        // 주전자 재고 3개는 이미 선점됐다
        cartItemRepository.deleteAll()
        addToCart(kettle.id!!, 1)

        place().andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("ORDER_001") }
        }

        assertThat(reservedStockOf(kettle)).isEqualTo(3)
        assertThat(orderRepository.count()).isEqualTo(1L)
    }

    @Test
    fun `품절 상품은 주문에서 빠진다`() {
        addToCart(water.id!!, 1)
        addToCart(soldOut.id!!, 1)

        place().andExpect {
            status { isCreated() }
            jsonPath("$.totalAmount") { value(7_800) }
        }

        val order = orderRepository.findAllByUserIdOrderByIdDesc(userId).first()
        assertThat(order.items).hasSize(1)
        assertThat(order.items.first().productName).isEqualTo("생수")
    }

    @Test
    fun `주문 상품은 주문 시점의 이름과 가격으로 남는다`() {
        addToCart(water.id!!, 1)
        place()

        val saved = productRepository.findById(water.id!!).get()
        saved.changePrice(9_900)
        productRepository.save(saved)

        val order = orderRepository.findAllByUserIdOrderByIdDesc(userId).first()
        assertThat(order.items.first().price).isEqualTo(4_800)
        assertThat(order.itemAmount).isEqualTo(4_800)
    }

    @Test
    fun `3만원을 넘으면 배송비가 0 이다`() {
        addToCart(kettle.id!!, 1)

        place().andExpect {
            jsonPath("$.totalAmount") { value(45_000) }
        }

        val order = orderRepository.findAllByUserIdOrderByIdDesc(userId).first()
        assertThat(order.shippingFee).isEqualTo(0)
        assertThat(order.status).isEqualTo(OrderStatus.PENDING)
    }

    @Test
    fun `배송지를 저장하면 전화번호가 암호문으로 남는다`() {
        addToCart(water.id!!, 1)

        place(saveAddress = true).andExpect {
            status { isCreated() }
        }

        val address = addressRepository.findByUserIdAndIsDefaultTrue(userId)!!
        assertThat(address.phoneEnc).doesNotContain("1234", "5678")
        assertThat(phoneCipher.decrypt(address.phoneEnc)).isEqualTo("010-1234-5678")
    }

    @Test
    fun `휴대폰 번호 형식이 아니면 400`() {
        addToCart(water.id!!, 1)

        mockMvc.post("/api/orders") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "receiver": "홍길동",
                  "phone": "12345",
                  "zipcode": "06236",
                  "addr1": "서울 강남구 테헤란로 1"
                }
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("COMMON_001") }
        }
    }
}
