package com.kassa.user

import com.kassa.cart.repository.CartItemRepository
import com.kassa.catalog.domain.Category
import com.kassa.catalog.domain.Product
import com.kassa.catalog.repository.CategoryRepository
import com.kassa.catalog.repository.ProductRepository
import com.kassa.support.FakeEmailSender
import com.kassa.support.IntegrationTest
import com.kassa.user.repository.EmailTokenRepository
import com.kassa.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@AutoConfigureMockMvc
class WithdrawTest : IntegrationTest() {

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
    private lateinit var productRepository: ProductRepository

    @Autowired
    private lateinit var categoryRepository: CategoryRepository

    private val email = "a@example.com"

    @BeforeEach
    fun setUp() {
        cartItemRepository.deleteAll()
        emailTokenRepository.deleteAll()
        userRepository.deleteAll()
        mailSender.clear()
    }

    private fun signUp(loginId: String = "hong01", to: String = email): ResultActionsDsl =
        mockMvc.post("/api/users") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"loginId":"$loginId","email":"$to","password":"abcd1234","name":"홍길동"}"""
        }

    private fun signUpAndVerify(loginId: String = "hong01", to: String = email) {
        signUp(loginId, to)
        mockMvc.post("/api/auth/email-verification/confirm") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"token":"${mailSender.lastTokenTo(to)}"}"""
        }
        mailSender.clear()
    }

    private fun login(loginId: String = "hong01", password: String = "abcd1234"): ResultActionsDsl =
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"loginId":"$loginId","password":"$password"}"""
        }

    private fun loginToken(): String {
        val body = login().andReturn().response.contentAsString
        return Regex("\"accessToken\":\"([^\"]+)\"").find(body)!!.groupValues[1]
    }

    private fun me(token: String): ResultActionsDsl = mockMvc.get("/api/users/me") {
        header("Authorization", "Bearer $token")
    }

    private fun withdraw(token: String, password: String = "abcd1234"): ResultActionsDsl =
        mockMvc.delete("/api/users/me") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"password":"$password"}"""
        }

    @Test
    fun `탈퇴하면 204 이고 개인정보가 지워진다`() {
        signUpAndVerify()
        val token = loginToken()
        withdraw(token).andExpect {
            status { isNoContent() }
        }
        assertThat(userRepository.count()).isEqualTo(1L)
        assertThat(userRepository.findByEmail(email)).isNull()
        assertThat(userRepository.findByLoginId("hong01")).isNull()
    }

    @Test
    fun `탈퇴한 회원의 토큰은 401`() {
        signUpAndVerify()
        val token = loginToken()
        withdraw(token)
        me(token).andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("AUTH_001") }
        }
    }

    @Test
    fun `탈퇴한 뒤 옛 아이디로 로그인하면 401 USER_006`() {
        signUpAndVerify()
        val token = loginToken()
        withdraw(token)

        login().andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("USER_006") }
        }
    }

    @Test
    fun `틀린 비밀번호로는 탈퇴되지 않는다`() {
        signUpAndVerify()
        val token = loginToken()
        withdraw(token, password = "wrong1234").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("USER_005") }
        }
        assertThat(userRepository.findByEmail(email)).isNotNull()

        me(token).andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `탈퇴하면 남은 메일 토큰도 지워진다`() {
        signUpAndVerify()
        mockMvc.post("/api/auth/password-reset") { contentType = MediaType.APPLICATION_JSON; content = """{"email":"$email"}""" }
        assertThat(emailTokenRepository.count()).isEqualTo(2L)
        withdraw(loginToken())
        assertThat(emailTokenRepository.count()).isEqualTo(0L)
    }

    @Test
    fun `탈퇴한 아이디와 이메일로 다시 가입할 수 있다`() {
        signUpAndVerify()
        withdraw(loginToken())
        signUp().andExpect {
            status { isAccepted() }
        }
        assertThat(userRepository.count()).isEqualTo(2L)
        assertThat(userRepository.findByEmail(email)!!.isEmailVerified()).isFalse()
    }

    @Test
    fun `deleted_ 로 시작하는 아이디로는 가입할 수 없다`() {
        signUp(loginId = "deleted_1").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("COMMON_001") }
        }
    }
    @Test
    fun `탈퇴하면 장바구니도 비워진다`() {
        signUpAndVerify()
        val token = loginToken()
        val product = productRepository.save(Product(categoryRepository.save(Category("음료")), "생수", 4_800))

        mockMvc.post("/api/cart/items") {
            header("Authorization", "Bearer " + token)
            contentType = MediaType.APPLICATION_JSON
            content = """{"productId":${product.id},"quantity":2}"""
        }
        assertThat(cartItemRepository.count()).isEqualTo(1L)

        withdraw(token)

        assertThat(cartItemRepository.count()).isEqualTo(0L)
    }
}
