package com.kassa.admin

import com.kassa.catalog.domain.Category
import com.kassa.catalog.domain.Product
import com.kassa.catalog.repository.CategoryRepository
import com.kassa.catalog.repository.ProductRepository
import com.kassa.common.crypto.PhoneCipher
import com.kassa.common.security.TokenIssuer
import com.kassa.order.domain.Order
import com.kassa.order.domain.OrderItem
import com.kassa.order.domain.ShippingInfo
import com.kassa.order.repository.OrderRepository
import com.kassa.payment.domain.Payment
import com.kassa.payment.repository.PaymentRepository
import com.kassa.saga.SagaRecorder
import com.kassa.saga.domain.SagaInstance
import com.kassa.saga.repository.SagaInstanceRepository
import com.kassa.support.IntegrationTest
import com.kassa.support.MutableClock
import com.kassa.support.MutableClockConfig
import com.kassa.user.domain.Role
import com.kassa.user.domain.User
import com.kassa.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import java.time.Duration
import java.time.Instant

@AutoConfigureMockMvc
@Import(MutableClockConfig::class)
class AdminAttentionControllerTest : IntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var orderRepository: OrderRepository
    @Autowired private lateinit var paymentRepository: PaymentRepository
    @Autowired private lateinit var instanceRepository: SagaInstanceRepository
    @Autowired private lateinit var productRepository: ProductRepository
    @Autowired private lateinit var categoryRepository: CategoryRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var passwordEncoder: PasswordEncoder
    @Autowired private lateinit var phoneCipher: PhoneCipher
    @Autowired private lateinit var tokenIssuer: TokenIssuer
    @Autowired private lateinit var clock: MutableClock

    private lateinit var water: Product
    private lateinit var adminToken: String
    private lateinit var userToken: String
    private var userId: Long = 0

    @BeforeEach
    fun setUp() {
        clock.reset()
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

    private fun order(orderNo: String): Order {
        val shipping = ShippingInfo(
            receiver = "홍길동",
            phoneEnc = phoneCipher.encrypt("010-1234-5678"),
            zipcode = "06236",
            addr1 = "서울 강남구 테헤란로 1",
            addr2 = null,
        )
        val order = Order(orderNo, userId, water.price, 3_000, shipping)
        order.addItem(OrderItem.of(water, 1))
        return orderRepository.save(order)
    }

    private fun attention(token: String = adminToken): ResultActionsDsl =
        mockMvc.get("/api/admin/attention") {
            header("Authorization", "Bearer $token")
        }

    @Test
    fun `일반 회원은 403`() {
        attention(userToken).andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `결과를 모르는 채 10분이 지난 결제가 나온다`() {
        val order = order("20261001-AAAAAAAA")
        paymentRepository.save(Payment(order.id!!, order.totalAmount))

        clock.advance(Duration.ofMinutes(11))

        attention().andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].orderNo") { value("20261001-AAAAAAAA") }
            jsonPath("$[0].reason") { value("PAYMENT_UNKNOWN") }
        }
    }

    @Test
    fun `10분이 안 된 결제는 나오지 않는다`() {
        val order = order("20261001-AAAAAAAA")
        paymentRepository.save(Payment(order.id!!, order.totalAmount))

        clock.advance(Duration.ofMinutes(5))

        attention().andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }
    }

    @Test
    fun `재시도 상한으로 멈춘 사가가 나온다`() {
        order("20261001-BBBBBBBB")
        val saga = SagaInstance(SagaRecorder.SAGA_TYPE, "20261001-BBBBBBBB")
        saga.giveUp(Instant.now())
        instanceRepository.save(saga)

        attention().andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].orderNo") { value("20261001-BBBBBBBB") }
            jsonPath("$[0].reason") { value("SAGA_STOPPED") }
        }
    }
}
