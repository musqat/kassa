package com.kassa.user

import com.kassa.cart.repository.CartItemRepository
import com.kassa.support.FakeEmailSender
import com.kassa.support.IntegrationTest
import com.kassa.support.MutableClock
import com.kassa.support.MutableClockConfig
import com.kassa.user.repository.EmailTokenRepository
import com.kassa.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.Duration

@AutoConfigureMockMvc
@Import(MutableClockConfig::class)
class ProfileTest : IntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var cartItemRepository: CartItemRepository

    @Autowired
    private lateinit var emailTokenRepository: EmailTokenRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var mailSender: FakeEmailSender

    @Autowired
    private lateinit var clock: MutableClock

    private val email = "a@example.com"

    @BeforeEach
    fun setUp() {
        cartItemRepository.deleteAll()
        emailTokenRepository.deleteAll()
        userRepository.deleteAll()
        mailSender.clear()
        clock.reset()
    }

    private fun signUp(): ResultActionsDsl = mockMvc.post("/api/users") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"loginId":"hong01","email":"$email","password":"abcd1234","name":"홍길동"}"""
    }

    private fun signUpAndVerify() {
        signUp()
        mockMvc.post("/api/auth/email-verification/confirm") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"token":"${mailSender.lastTokenTo(email)}"}"""
        }
        mailSender.clear()
    }

    private fun login(password: String = "abcd1234"): ResultActionsDsl = mockMvc.post("/api/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"loginId":"hong01","password":"$password"}"""
    }

    private fun loginToken(password: String = "abcd1234"): String {
        val body = login(password).andReturn().response.contentAsString
        return Regex("\"accessToken\":\"([^\"]+)\"").find(body)!!.groupValues[1]
    }

    private fun me(token: String): ResultActionsDsl = mockMvc.get("/api/users/me") {
        header("Authorization", "Bearer $token")
    }

    private fun changeName(name: String, token: String? = null): ResultActionsDsl =
        mockMvc.patch("/api/users/me") {
            if (token != null) header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"$name"}"""
        }

    private fun changePassword(
        token: String,
        currentPassword: String = "abcd1234",
        newPassword: String = "newpass123",
    ): ResultActionsDsl = mockMvc.patch("/api/users/me/password") {
        header("Authorization", "Bearer $token")
        contentType = MediaType.APPLICATION_JSON
        content = """{"currentPassword":"$currentPassword","newPassword":"$newPassword"}"""
    }

    @Test
    fun `이름을 바꾸면 조회에 반영된다`() {
        signUpAndVerify()
        val token = loginToken()

        changeName("김철수", token).andExpect {
            status { isNoContent() }
        }

        me(token).andExpect {
            status { isOk() }
            jsonPath("$.name") { value("김철수") }
        }
    }

    @Test
    fun `이름 앞뒤 공백은 지운다`() {
        signUpAndVerify()
        val token = loginToken()

        changeName("  김철수  ", token)

        assertThat(userRepository.findByLoginId("hong01")!!.name).isEqualTo("김철수")
    }

    @Test
    fun `빈 이름은 400`() {
        signUpAndVerify()
        val token = loginToken()

        changeName(" ", token).andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("COMMON_001") }
        }
    }

    @Test
    fun `51자 이름은 400`() {
        signUpAndVerify()
        val token = loginToken()

        changeName("가".repeat(51), token).andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("COMMON_001") }
        }
    }

    @Test
    fun `토큰 없이 이름을 바꾸면 401`() {
        changeName("김철수").andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("AUTH_001") }
        }
    }

    @Test
    fun `비밀번호를 바꾸면 새 비밀번호로 로그인된다`() {
        signUpAndVerify()
        val token = loginToken()

        changePassword(token).andExpect {
            status { isNoContent() }
        }

        login("newpass123").andExpect {
            status { isOk() }
        }
        login().andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("USER_002") }
        }
    }

    @Test
    fun `바꾸면 그 전에 받은 토큰은 401`() {
        signUpAndVerify()
        val token = loginToken()
        clock.advance(Duration.ofSeconds(1))

        changePassword(token)

        me(token).andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("AUTH_001") }
        }

        me(loginToken("newpass123")).andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `지금 비밀번호가 틀리면 400 USER_005 이고 그대로다`() {
        signUpAndVerify()
        val token = loginToken()

        changePassword(token, currentPassword = "wrong1234").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("USER_005") }
        }

        login().andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `새 비밀번호가 규칙에 안 맞으면 400 COMMON_001`() {
        signUpAndVerify()
        val token = loginToken()

        changePassword(token, newPassword = "12345678").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("COMMON_001") }
        }
    }

    @Test
    fun `비밀번호를 바꿔도 인증 시각은 그대로다`() {
        signUpAndVerify()
        val verifiedAt = userRepository.findByLoginId("hong01")!!.emailVerifiedAt
        val token = loginToken()
        clock.advance(Duration.ofMinutes(5))

        changePassword(token)

        val user = userRepository.findByLoginId("hong01")!!
        assertThat(user.emailVerifiedAt).isEqualTo(verifiedAt)
        assertThat(user.isEmailVerified()).isTrue()
    }
}
