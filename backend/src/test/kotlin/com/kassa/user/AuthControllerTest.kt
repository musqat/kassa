package com.kassa.user

import com.kassa.common.security.TokenIssuer
import com.kassa.support.IntegrationTest
import com.kassa.support.MutableClock
import com.kassa.support.MutableClockConfig
import com.kassa.user.domain.User
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

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var passwordEncoder: PasswordEncoder
    @Autowired private lateinit var tokenIssuer: TokenIssuer
    @Autowired private lateinit var clock: MutableClock

    private lateinit var user: User

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
        clock.reset()
        user = userRepository.save(User("a@example.com", passwordEncoder.encode("abcd1234")!!, "홍길동"))
    }

    private fun login(email: String = "a@example.com", password: String = "abcd1234"): ResultActionsDsl =
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"$email","password":"$password"}"""
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
            jsonPath("$.email") {value("a@example.com")}
        }
    }

    @Test
    fun `틀린 비밀번호는 401 USER_002`() {
        login(password = "wrong123").andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("USER_002")}
        }
    }

    @Test
    fun `없는 이메일도 같은 응답이다`() {
        login(email = "nobody@example.com").andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("USER_002")}
        }
    }

    @Test
    fun `5회 틀리면 맞는 비밀번호도 막힌다`() {
        repeat(5){ login(password = "wrong1234")}
        login().andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") {value("USER_002")}
        }
    }

    @Test
    fun `없는 이메일은 여러 번 틀려도 응답이 같다`() {
        repeat(5) { login(email = "nobody@example.com") }
        login(email = "nobody@example.com").andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") {value("USER_002")}
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
            jsonPath("$.code") {value("AUTH_001")}
        }

    }

    @Test
    fun `토큰이 있어도 상품 조회는 된다`() {
        val token = tokenIssuer.issue(user.id!!, Instant.now()).value
        mockMvc.get("/api/products"){
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
        }
    }
}
