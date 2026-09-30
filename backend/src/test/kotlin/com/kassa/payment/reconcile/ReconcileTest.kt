package com.kassa.payment.reconcile

import com.kassa.catalog.domain.Category
import com.kassa.catalog.domain.Product
import com.kassa.catalog.repository.CategoryRepository
import com.kassa.catalog.repository.ProductRepository
import com.kassa.common.security.TokenIssuer
import com.kassa.order.repository.OrderRepository
import com.kassa.payment.gateway.FakePaymentGateway
import com.kassa.payment.repository.PaymentRepository
import com.kassa.support.IntegrationTest
import com.kassa.user.domain.Role
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.time.LocalDate

@AutoConfigureMockMvc
class ReconcileTest : IntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var orderRepository: OrderRepository
    @Autowired private lateinit var productRepository: ProductRepository
    @Autowired private lateinit var categoryRepository: CategoryRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var paymentRepository: PaymentRepository
    @Autowired private lateinit var diffRepository: ReconcileDiffRepository
    @Autowired private lateinit var passwordEncoder: PasswordEncoder
    @Autowired private lateinit var tokenIssuer: TokenIssuer
    @Autowired private lateinit var gateway: FakePaymentGateway
    @Autowired private lateinit var reconcileService: ReconcileService

    private lateinit var water: Product
    private lateinit var token: String

    // 대조할 날짜. 오늘 만든 결제가 이 날짜에 들어간다
    private val today: LocalDate = LocalDate.now(ReconcileService.KST)

    @BeforeEach
    fun setUp() {
        gateway.reset()

        val category = categoryRepository.save(Category("음료"))
        water = productRepository.save(Product(category, "생수", 4_800, stock = 100))

        val user = userRepository.save(
            User("hong01", "a@example.com", passwordEncoder.encode("abcd1234")!!, "홍길동")
                .apply { verifyEmail(Instant.now()) },
        )
        token = tokenIssuer.issue(user.id!!, Role.USER, Instant.now()).value
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

        return Regex("\"orderNo\":\"([^\"]+)\"").find(body)!!.groupValues[1]
    }

    /** 주문하고 승인까지 마친다. 양쪽에 결제가 남는다 */
    private fun paidOrder(quantity: Int = 2): String {
        val orderNo = placeOrder(quantity)
        mockMvc.post("/api/payments/confirm") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"orderNo":"$orderNo"}"""
        }

        return orderNo
    }

    @Test
    fun `양쪽이 같으면 불일치가 없다`() {
        paidOrder()

        val found = reconcileService.reconcile(today)

        assertThat(found).isEqualTo(0)
        assertThat(diffRepository.findAll()).isEmpty()
    }

    @Test
    fun `대행사에만 있는 결제는 MISSING_LOCAL`() {
        gateway.plant("20260929-NOORDER", 10_000, Instant.now())

        val found = reconcileService.reconcile(today)

        assertThat(found).isEqualTo(1)

        val diff = diffRepository.findAll().single()
        assertThat(diff.kind).isEqualTo(DiffKind.MISSING_LOCAL)
        assertThat(diff.orderNo).isEqualTo("20260929-NOORDER")
        assertThat(diff.gatewaySide).contains("10000")
        assertThat(diff.localSide).isNull()
    }

    @Test
    fun `내 쪽에만 PAID 인 결제는 MISSING_GATEWAY`() {
        val orderNo = paidOrder()

        // 대행사 기록만 지운다. 내 Payment 는 PAID 로 남는다
        gateway.reset()

        val found = reconcileService.reconcile(today)

        assertThat(found).isEqualTo(1)

        val diff = diffRepository.findAll().single()
        assertThat(diff.kind).isEqualTo(DiffKind.MISSING_GATEWAY)
        assertThat(diff.orderNo).isEqualTo(orderNo)
        assertThat(diff.gatewaySide).isNull()
        assertThat(diff.localSide).contains("12600")
    }

    @Test
    fun `금액이 다르면 AMOUNT_MISMATCH 이고 양쪽 값이 남는다`() {
        val orderNo = paidOrder()

        // 같은 주문번호에 다른 금액을 덮어쓴다
        gateway.plant(orderNo, 9_900, Instant.now())

        val found = reconcileService.reconcile(today)

        assertThat(found).isEqualTo(1)

        val diff = diffRepository.findAll().single()
        assertThat(diff.kind).isEqualTo(DiffKind.AMOUNT_MISMATCH)
        assertThat(diff.gatewaySide).contains("9900")
        assertThat(diff.localSide).contains("12600")
    }

    @Test
    fun `같은 날을 두 번 대조해도 같은 건이 쌓이지 않는다`() {
        gateway.plant("20260929-NOORDER", 10_000, Instant.now())

        assertThat(reconcileService.reconcile(today)).isEqualTo(1)
        assertThat(reconcileService.reconcile(today)).isEqualTo(0)

        assertThat(diffRepository.count()).isEqualTo(1)
    }

    @Test
    fun `조회 API 는 그날 것만 준다`() {
        gateway.plant("20260929-NOORDER", 10_000, Instant.now())
        reconcileService.reconcile(today)

        mockMvc.get("/api/admin/reconcile") {
            param("date", today.toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
            jsonPath("$[0].kind") { value("MISSING_LOCAL") }
        }

        mockMvc.get("/api/admin/reconcile") {
            param("date", today.minusDays(1).toString())
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(0) }
        }
    }
}
