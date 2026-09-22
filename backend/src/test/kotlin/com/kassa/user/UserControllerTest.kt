package com.kassa.user

import com.kassa.support.IntegrationTest
import com.kassa.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post

@AutoConfigureMockMvc
class UserControllerTest : IntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        userRepository.deleteAll()
    }

    private fun signUp(
        email: String = "a@example.com",
        password: String = "abcd1234",
        name: String = "홍길동",
    ): ResultActionsDsl = mockMvc.post("/api/users") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"email":"$email","password":"$password","name":"$name"}"""
    }

    @Test
    fun `가입하면 201 과 회원 정보를 준다`() {
        signUp().andExpect {
            status { isCreated() }
            jsonPath("$.email") { value("a@example.com") }
            jsonPath("$.password") { doesNotExist() }
            jsonPath("$.passwordHash") { doesNotExist() }
        }
    }

    @Test
    fun `비밀번호는 BCrypt 해시로 저장된다`() {
        signUp()

        val saved = userRepository.findByEmail("a@example.com")!!
        assertThat(saved.passwordHash).startsWith("\$2a\$")
    }

    @Test
    fun `같은 이메일로 다시 가입하면 409`() {
        signUp()

        signUp(email = "a@example.com").andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("USER_001") }
        }
    }

    @Test
    fun `대문자만 다른 이메일도 중복이다`() {
        signUp()

        signUp(email = "A@example.com").andExpect {
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
}
