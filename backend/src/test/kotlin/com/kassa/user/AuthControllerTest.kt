package com.kassa.user

import com.kassa.common.security.TokenIssuer
import com.kassa.cart.repository.CartItemRepository
import com.kassa.order.repository.AddressRepository
import com.kassa.order.repository.OrderRepository
import com.kassa.support.IntegrationTest
import com.kassa.support.MutableClock
import com.kassa.support.MutableClockConfig
import com.kassa.user.domain.User
import com.kassa.user.repository.EmailTokenRepository
import com.kassa.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Duration
import java.time.Instant

@AutoConfigureMockMvc
@Import(MutableClockConfig::class)
class AuthControllerTest : IntegrationTest() {
    @Autowired
    private lateinit var cartItemRepository: CartItemRepository

    @Autowired
    private lateinit var orderRepository: OrderRepository

    @Autowired
    private lateinit var addressRepository: AddressRepository

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var tokenIssuer: TokenIssuer

    @Autowired
    private lateinit var clock: MutableClock

    @Autowired
    private lateinit var emailTokenRepository: EmailTokenRepository

    private lateinit var user: User

    @BeforeEach
    fun setUp() {
        orderRepository.deleteAll()
        addressRepository.deleteAll()
        cartItemRepository.deleteAll()
        emailTokenRepository.deleteAll()
        userRepository.deleteAll()
        clock.reset()
        user = userRepository.save(
            User(
                "hong01",
                "a@example.com",
                passwordEncoder.encode("abcd1234")!!,
                "홍길동"
            ).apply {verifyEmail(Instant.now())}

        )
    }

    private fun login(loginId: String = "hong01", password: String = "abcd1234"): ResultActionsDsl =
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"loginId":"$loginId","password":"$password"}"""
        }

    private fun me(token: String? = null): ResultActionsDsl =
        mockMvc.get("/api/users/me") {
            if (token != null) header("Authorization", "Bearer $token")
        }

    @Test
    fun `맞는 비밀번호면 토큰을 준다`() {
        login().andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { isNotEmpty() }
            jsonPath("$.expiresIn") { value(7200) }
        }
    }

    @Test
    fun `토큰 없이 보호된 경로는 401`() {
        me().andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("AUTH_001") }
            jsonPath("$.requestId") { isNotEmpty() }
        }
    }

    @Test
    fun `토큰이 있으면 내 정보를 준다`() {
        val token = tokenIssuer.issue(user.id!!, Instant.now()).value
        me(token).andExpect {
            status { isOk() }
            jsonPath("$.email") { value("a@example.com") }
        }
    }

    @Test
    fun `틀린 비밀번호는 401 USER_002 와 남은 횟수`() {
        login(password = "wrong123").andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("USER_002") }
            jsonPath("$.detail") { value("비밀번호가 맞지 않습니다. 4회 더 틀리면 15분간 로그인이 제한됩니다") }
        }
    }

    @Test
    fun `없는 아이디는 401 USER_006`() {
        login(loginId = "nobody").andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("USER_006") }
        }
    }

    @Test
    fun `5회째 틀리면 423 으로 잠긴다`() {
        repeat(4) { login(password = "wrong1234") }
        login(password = "wrong1234").andExpect {
            status { isLocked() }
            jsonPath("$.code") { value("USER_007") }
            jsonPath("$.detail") { value("로그인 시도가 많아 제한됐습니다. 15분 뒤 다시 시도하세요") }
        }
    }

    @Test
    fun `잠긴 동안에는 맞는 비밀번호도 423`() {
        repeat(5) { login(password = "wrong1234") }
        clock.advance(Duration.ofMinutes(10))
        login().andExpect {
            status { isLocked() }
            jsonPath("$.code") { value("USER_007") }
            jsonPath("$.detail") { value("로그인 시도가 많아 제한됐습니다. 5분 뒤 다시 시도하세요") }
        }
    }

    @Test
    fun `잠긴 뒤 16분이 지나면 로그인된다`() {
        repeat(5) { login(password = "wrong1234") }
        clock.advance(Duration.ofMinutes(16))
        login().andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `만료된 토큰은 401`() {
        val expiredToken = tokenIssuer.issue(user.id!!, Instant.now().minus(Duration.ofHours(3))).value
        me(token = expiredToken).andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("AUTH_001") }
        }
    }

    @Test
    fun `토큰이 있어도 상품 조회는 된다`() {
        val token = tokenIssuer.issue(user.id!!, Instant.now()).value
        mockMvc.get("/api/products") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
        }
    }

    // 미인증 회원. setUp 의 회원과 달리 verifyEmail 을 부르지 않는다
    private fun saveUnverified(): User =
        userRepository.save(User("hong02", "b@example.com", passwordEncoder.encode("abcd1234")!!, "김철수"))

    @Test
    fun `미인증 회원은 맞는 비밀번호여도 403 USER_003`() {
        saveUnverified()
        login(loginId = "hong02").andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("USER_003") }
        }
    }

    @Test
    fun `미인증 회원도 틀린 비밀번호는 401 USER_002`() {
        saveUnverified()
        login(loginId = "hong02", password = "wrong1234").andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("USER_002") }
        }
    }
}
