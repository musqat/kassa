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
import org.springframework.test.web.servlet.post
import java.time.Duration

@AutoConfigureMockMvc
@Import(MutableClockConfig::class)
class EmailVerificationTest : IntegrationTest() {

    @Autowired
    private lateinit var cartItemRepository: CartItemRepository

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

    private fun resend(to: String = email): ResultActionsDsl = mockMvc.post("/api/auth/email-verification") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"email":"$to"}"""
    }

    private fun confirm(token: String): ResultActionsDsl = mockMvc.post("/api/auth/email-verification/confirm") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"token":"$token"}"""
    }

    private fun login(): ResultActionsDsl = mockMvc.post("/api/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"loginId":"hong01","password":"abcd1234"}"""
    }

    @Test
    fun `재전송하면 새 링크가 오고 옛 링크는 400`() {
        signUp()
        val oldToken = mailSender.lastTokenTo(email)

        clock.advance(Duration.ofMinutes(1))
        resend().andExpect {
            status { isAccepted() }
        }

        assertThat(mailSender.countTo(email)).isEqualTo(2)

        confirm(oldToken).andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("USER_004") }
        }
        confirm(mailSender.lastTokenTo(email)).andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `1분 안에 재전송하면 202 지만 메일은 보내지 않는다`() {
        signUp()
        resend().andExpect {
            status { isAccepted() }
        }
        resend().andExpect {
            status { isAccepted() }
        }

        assertThat(mailSender.countTo(email)).isEqualTo(1)
    }

    @Test
    fun `인증된 회원이 재전송하면 메일을 보내지 않는다`() {
        signUp()
        confirm(mailSender.lastTokenTo(email))

        clock.advance(Duration.ofMinutes(1))
        resend().andExpect {
            status { isAccepted() }
        }

        assertThat(mailSender.countTo(email)).isEqualTo(1)
    }
    @Test
    fun `없는 이메일로 재전송해도 202, 메일은 없다`() {
        resend("nobody@example.com").andExpect {
            status { isAccepted() }
        }
        assertThat(mailSender.sent).isEmpty()
    }

    @Test
    fun `같은 링크를 두 번 쓰면 두 번째는 400`() {
        signUp()
        val token = mailSender.lastTokenTo(email)

        confirm(token).andExpect {
            status { isNoContent() }
        }

        confirm(token).andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("USER_004") }
        }
    }

    @Test
    fun `24시간이 지난 링크는 400`() {
        signUp()
        val token = mailSender.lastTokenTo(email)
        clock.advance(Duration.ofHours(24))

        confirm(token).andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("USER_004") }
        }
    }

    @Test
    fun `인증하면 로그인된다`() {
        signUp()
        login().andExpect {
            status { isForbidden() }
        }
        confirm(mailSender.lastTokenTo(email))

        login().andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `1시간에 5통까지만 보낸다`() {
        signUp()
        repeat(4) {
            clock.advance(Duration.ofMinutes(1))
            resend()
        }
        clock.advance(Duration.ofMinutes(1))

        resend().andExpect {
            status { isAccepted() }
        }
        assertThat(mailSender.countTo(email)).isEqualTo(5)
        clock.advance(Duration.ofHours(1))
        resend()
        assertThat(mailSender.countTo(email)).isEqualTo(6)
    }
}
