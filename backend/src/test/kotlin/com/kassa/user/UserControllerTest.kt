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
class UserControllerTest : IntegrationTest() {
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

    @BeforeEach
    fun setUp() {
        emailTokenRepository.deleteAll()
        userRepository.deleteAll()
        mailSender.clear()
        clock.reset()
    }

    private fun confirm(token: String): ResultActionsDsl = mockMvc.post("/api/auth/email-verification/confirm") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"token":"$token"}"""
    }

    // 가입하고 받은 메일의 링크로 인증까지 끝낸다
    private fun signUpAndVerify(loginId: String = "hong01", email: String = "a@example.com") {
        signUp(loginId = loginId, email = email)
        confirm(mailSender.lastTokenTo(email))
    }

    private fun signUp(
        loginId: String = "hong01",
        email: String = "a@example.com",
        password: String = "abcd1234",
        name: String = "홍길동",
    ): ResultActionsDsl = mockMvc.post("/api/users") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"loginId":"$loginId","email":"$email","password":"$password","name":"$name"}"""
    }

    @Test
    fun `가입하면 202 와 인증 메일 1통, 회원은 미인증`() {
        signUp().andExpect { status { isAccepted() } }

        assertThat(mailSender.countTo("a@example.com")).isEqualTo(1)

        val user = userRepository.findByEmail("a@example.com")!!
        assertThat(user.isEmailVerified()).isFalse()
    }

    @Test
    fun `비밀번호는 BCrypt 해시로 저장된다`() {
        signUp()

        val saved = userRepository.findByEmail("a@example.com")!!
        assertThat(saved.passwordHash).startsWith("\$2a\$")
    }

    @Test
    fun `인증된 이메일로 다시 가입하면 409`() {
        signUpAndVerify()

        signUp(loginId = "hong02", email = "a@example.com").andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("USER_001") }
        }
    }

    @Test
    fun `대문자만 다른 이메일도 중복이다`() {
        signUpAndVerify()

        signUp(loginId = "hong02", email = "A@example.com").andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("USER_001") }
        }
    }

    @Test
    fun `비밀번호가 7자면 400`() {
        signUp(password = "abc123").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("COMMON_001") }
        }
    }

    @Test
    fun `숫자만 있는 비밀번호는 400`() {
        signUp(password = "12345678").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("COMMON_001") }
        }
    }

    @Test
    fun `같은 아이디로 가입하면 409 USER_008`() {
        signUpAndVerify()

        signUp(email = "b@example.com").andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("USER_008") }
        }
    }

    @Test
    fun `아이디 규칙에 안 맞으면 400`() {
        listOf("abc", "Hong01", "hong-01", "a".repeat(21)).forEach { id ->
            signUp(loginId = id).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("COMMON_001") }
            }
        }
    }

    @Test
    fun `아이디 중복확인`() {
        signUpAndVerify()

        availability("hong01").andExpect {
            status { isOk() }
            jsonPath("$.available") { value(false) }
        }
        availability("hong02").andExpect {
            status { isOk() }
            jsonPath("$.available") { value(true) }
        }
    }

    @Test
    fun `중복확인도 아이디 규칙을 검사한다`() {
        availability("AB").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("COMMON_001") }
            jsonPath("$.detail") { value("loginId: 4~20자, 영문 소문자·숫자·_ 만 쓸 수 있습니다") }
        }
    }

    @Test
    fun `미인증 이메일로 다시 가입하면 새 값으로 덮고 옛 링크는 죽는다`() {
        signUp()
        val oldToken = mailSender.lastTokenTo("a@example.com")

        clock.advance(Duration.ofMinutes(1))

        signUp(loginId = "hong02", password = "newpass123").andExpect {
            status { isAccepted() }
        }

        val hongUser = userRepository.findByEmail("a@example.com")!!
        assertThat(hongUser.loginId).isEqualTo("hong02")
        assertThat(userRepository.count()).isEqualTo(1L)

        confirm(oldToken).andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("USER_004") }
        }
        confirm(mailSender.lastTokenTo("a@example.com")).andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `미인증 회원이 쥔 아이디는 다른 이메일로 가입하면 풀린다`() {
        signUp(email = "typo@exmaple.com")
        signUp(email = "a@example.com").andExpect {
            status { isAccepted() }
        }
        assertThat(userRepository.findByLoginId("hong01")!!.email).isEqualTo("a@example.com")
        assertThat(userRepository.findByEmail("typo@exmaple.com")).isNull()
        assertThat(userRepository.count()).isEqualTo(1L)
    }

    @Test
    fun `메일 발송이 실패하면 503 MAIL_001 이고 회원은 남지 않는다`() {
        mailSender.failNext = true
        signUp().andExpect {
            status { isServiceUnavailable() }
            jsonPath("$.code") { value("MAIL_001") }
        }

        assertThat(userRepository.count()).isEqualTo(0L)
        assertThat(emailTokenRepository.count()).isEqualTo(0L)

        signUp().andExpect {
            status { isAccepted() }
        }
    }

    private fun availability(loginId: String): ResultActionsDsl =
        mockMvc.get("/api/users/login-id-check") { param("loginId", loginId) }
}
