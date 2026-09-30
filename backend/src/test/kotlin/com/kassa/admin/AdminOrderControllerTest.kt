package com.kassa.admin

import com.kassa.catalog.domain.Category
import com.kassa.catalog.domain.Product
import com.kassa.catalog.repository.CategoryRepository
import com.kassa.catalog.repository.ProductRepository
import com.kassa.common.crypto.PhoneCipher
import com.kassa.common.security.TokenIssuer
import com.kassa.order.domain.Order
import com.kassa.order.domain.OrderItem
import com.kassa.order.domain.OrderStatus
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
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch

@AutoConfigureMockMvc
class AdminOrderControllerTest : IntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var orderRepository: OrderRepository
    @Autowired private lateinit var productRepository: ProductRepository
    @Autowired private lateinit var categoryRepository: CategoryRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var passwordEncoder: PasswordEncoder
    @Autowired private lateinit var phoneCipher: PhoneCipher
    @Autowired private lateinit var tokenIssuer: TokenIssuer

    private lateinit var water: Product
    private lateinit var adminToken: String
    private lateinit var userToken: String
    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        val category = categoryRepository.save(Category("음료"))
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
        if (role == Role.USER) userId = user.id!!

        return tokenIssuer.issue(user.id!!, role, Instant.now()).value
    }

    /** 주문 하나를 만든다. paid 면 결제까지 끝난 상태로 둔다 */
    private fun order(orderNo: String, paid: Boolean = false): Order {
        val shipping = ShippingInfo(
            receiver = "홍길동",
            phoneEnc = phoneCipher.encrypt("010-1234-5678"),
            zipcode = "06236",
            addr1 = "서울 강남구 테헤란로 1",
            addr2 = null,
        )
        val order = Order(orderNo, userId, water.price, 3_000, shipping)
        order.addItem(OrderItem.of(water, 1))
        if (paid) order.markPaid(Instant.now())

        return orderRepository.save(order)
    }

    private fun orders(token: String = adminToken, status: String? = null): ResultActionsDsl =
        mockMvc.get("/api/admin/orders") {
            header("Authorization", "Bearer $token")
            if (status != null) param("status", status)
        }

    private fun ship(orderNo: String): ResultActionsDsl =
        mockMvc.patch("/api/admin/orders/$orderNo/ship") {
            header("Authorization", "Bearer $adminToken")
        }

    @Test
    fun `일반 회원은 403`() {
        orders(userToken).andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `관리자는 모든 회원의 주문을 본다`() {
        order("20261001-AAAAAAAA")
        order("20261001-BBBBBBBB", paid = true)

        orders().andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            // 최신순이라 나중에 만든 주문이 앞이다
            jsonPath("$[0].orderNo") { value("20261001-BBBBBBBB") }
            jsonPath("$[0].loginId") { value("hong01") }
        }
    }

    @Test
    fun `상태로 거를 수 있다`() {
        order("20261001-AAAAAAAA")
        order("20261001-BBBBBBBB", paid = true)

        orders(status = "PAID").andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].orderNo") { value("20261001-BBBBBBBB") }
        }
    }

    @Test
    fun `단건은 주문 줄과 전체 전화번호를 준다`() {
        order("20261001-AAAAAAAA", paid = true)

        mockMvc.get("/api/admin/orders/20261001-AAAAAAAA") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.order.orderNo") { value("20261001-AAAAAAAA") }
            jsonPath("$.items.length()") { value(1) }
            jsonPath("$.phone") { value("010-1234-5678") }
        }
    }

    @Test
    fun `없는 주문번호는 404`() {
        mockMvc.get("/api/admin/orders/20261001-ZZZZZZZZ") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("ORDER_002") }
        }
    }

    @Test
    fun `결제된 주문을 배송 처리한다`() {
        order("20261001-AAAAAAAA", paid = true)

        ship("20261001-AAAAAAAA").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("SHIPPED") }
            jsonPath("$.shippedAt") { exists() }
        }

        assertThat(orderRepository.findByOrderNo("20261001-AAAAAAAA")!!.status)
            .isEqualTo(OrderStatus.SHIPPED)
    }

    @Test
    fun `결제 전 주문은 409 ORDER_005`() {
        order("20261001-AAAAAAAA")

        ship("20261001-AAAAAAAA").andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("ORDER_005") }
        }

        assertThat(orderRepository.findByOrderNo("20261001-AAAAAAAA")!!.status)
            .isEqualTo(OrderStatus.PENDING)
    }

    @Test
    fun `이미 배송 처리한 주문은 409`() {
        order("20261001-AAAAAAAA", paid = true)
        ship("20261001-AAAAAAAA")

        ship("20261001-AAAAAAAA").andExpect {
            status { isConflict() }
        }
    }
}
