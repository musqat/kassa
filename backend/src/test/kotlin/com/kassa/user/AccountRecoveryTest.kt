package com.kassa.user

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
import org.springframework.test.web.servlet.post
import java.time.Duration

@AutoConfigureMockMvc
@Import(MutableClockConfig::class)
class AccountRecoveryTest : IntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var emailTokenRepository: EmailTokenRepository

    @Autowired
    private lateinit var mailSender: FakeEmailSender

    @Autowired
    private lateinit var clock: MutableClock

    private val email = "a@example.com"

    @BeforeEach
    fun setUp() {
        emailTokenRepository.deleteAll()
        userRepository.deleteAll()
        mailSender.clear()
        clock.reset()
    }

    private fun signUp(): ResultActionsDsl = mockMvc.post("/api/users") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"loginId":"hong01","email":"$email","password":"abcd1234","name":"홍길동"}"""
    }

    // 가입하고 인증까지 끝낸다
    private fun signUpAndVerify() {
        signUp()
        confirmVerification(mailSender.lastTokenTo(email))
        mailSender.clear()
    }

    private fun confirmVerification(token: String): ResultActionsDsl =
        mockMvc.post("/api/auth/email-verification/confirm") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"token":"$token"}"""
        }

    private fun findLoginId(to: String = email): ResultActionsDsl = mockMvc.post("/api/auth/login-id/find") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"email":"$to"}"""
    }

    private fun requestReset(to: String = email): ResultActionsDsl = mockMvc.post("/api/auth/password-reset") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"email":"$to"}"""
    }

    private fun confirmReset(token: String, newPassword: String = "newpass123"): ResultActionsDsl =
        mockMvc.post("/api/auth/password-reset/confirm") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"token":"$token","newPassword":"$newPassword"}"""
        }

    private fun login(password: String = "abcd1234"): ResultActionsDsl = mockMvc.post("/api/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"loginId":"hong01","password":"$password"}"""
    }

    private fun me(token: String): ResultActionsDsl = mockMvc.get("/api/users/me") {
        header("Authorization", "Bearer $token")
    }

    // 로그인해서 받은 토큰 문자열을 꺼낸다
    private fun loginToken(password: String = "abcd1234"): String {
        val body = login(password).andReturn().response.contentAsString
        return Regex("\"accessToken\":\"([^\"]+)\"").find(body)!!.groupValues[1]
    }

    @Test
    fun `아이디 찾기는 202 이고 메일에 아이디가 들어 있다`() {
        signUpAndVerify()
        findLoginId().andExpect {
            status { isAccepted() }
        }
        assertThat(mailSender.countTo(email)).isEqualTo(1)
        assertThat(mailSender.sent.last().body).contains("hong01")
    }

    @Test
    fun `미인증 회원과 없는 이메일은 아이디 찾기 메일을 받지 않는다`() {
        signUp()
        mailSender.clear()
        findLoginId().andExpect {
            status { isAccepted() }
        }
        findLoginId("nobody@example.com").andExpect {
            status { isAccepted() }
        }
        assertThat(mailSender.sent).isEmpty()
    }

    @Test
    fun `재설정 요청은 202 이고 메일 1통`() {
        signUpAndVerify()
        requestReset().andExpect {
            status { isAccepted() }
        }
        assertThat(mailSender.countTo(email)).isEqualTo(1)
    }

    @Test
    fun `없는 이메일로 재설정을 요청해도 202, 메일은 없다`() {
        requestReset("nobody@example.com").andExpect {
            status { isAccepted() }
        }
        assertThat(mailSender.sent).isEmpty()
    }

    @Test
    fun `재설정하면 새 비밀번호로 로그인되고 옛 비밀번호는 막힌다`() {
        signUpAndVerify()
        requestReset()
        confirmReset(mailSender.lastTokenTo(email)).andExpect {
            status { isNoContent() }
        }
        // 새로운 패스워드 설정
        login("newpass123").andExpect {
            status { isOk() }
        }
        // 과거 패스워드 - 실패
        login().andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("USER_002") }
        }
    }

    @Test
    fun `재설정하면 그 전에 받은 토큰은 401, 새로 받은 토큰은 200`() {
        signUpAndVerify()
        val oldToken = loginToken()
        me(oldToken).andExpect {
            status { isOk() }
        }
        clock.advance(Duration.ofSeconds(1))

        requestReset()
        confirmReset(mailSender.lastTokenTo(email))

        me(oldToken).andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("AUTH_001") }
        }

        val newToken = loginToken("newpass123")
        me(newToken).andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `31분이 지난 재설정 링크는 400`() {
        signUpAndVerify()
        requestReset()
        val token = mailSender.lastTokenTo(email)

        clock.advance(Duration.ofMinutes(31))

        confirmReset(token).andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("USER_004") }
        }
    }

    @Test
    fun `같은 재설정 링크는 두 번 쓸 수 없다`() {
        signUpAndVerify()
        requestReset()
        val token = mailSender.lastTokenTo(email)

        confirmReset(token).andExpect {
            status { isNoContent() }
        }

        confirmReset(token).andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("USER_004") }
        }
    }

    @Test
    fun `재설정을 다시 요청하면 앞의 링크는 죽는다`() {
        signUpAndVerify()
        requestReset()
        val oldToken = mailSender.lastTokenTo(email)

        clock.advance(Duration.ofMinutes(1))

        requestReset()

        confirmReset(oldToken).andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("USER_004") }
        }

        confirmReset(mailSender.lastTokenTo(email)).andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `잠긴 계정도 재설정하면 바로 로그인된다`() {
        signUpAndVerify()
        repeat(5) { login("wrong1234") }

        login().andExpect {
            status { isLocked() }
        }

        requestReset()
        confirmReset(mailSender.lastTokenTo(email))
        login("newpass123").andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `미인증 회원이 재설정하면 인증까지 끝난다`() {
        signUp()
        mailSender.clear()
        requestReset()
        confirmReset(mailSender.lastTokenTo(email))
        login("newpass123").andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `인증용 토큰으로는 비밀번호를 바꿀 수 없다`() {
        signUp()
        val verifyToken = mailSender.lastTokenTo(email)
        confirmReset(verifyToken).andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("USER_004") }
        }
    }

    @Test
    fun `새 비밀번호가 규칙에 안 맞으면 400 - COMMON_001`() {
        signUpAndVerify()
        requestReset()
        confirmReset(mailSender.lastTokenTo(email), newPassword = "12345678").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("COMMON_001") }
        }
    }

    @Test
    fun `재설정 요청도 1분 제한이 걸린다`() {
        signUpAndVerify()
        requestReset().andExpect {
            status { isAccepted() }
        }
        requestReset().andExpect {
            status { isAccepted() }
        }
        assertThat(mailSender.countTo(email)).isEqualTo(1)
    }
}
