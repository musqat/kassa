package com.kassa

import com.kassa.support.IntegrationTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
class OpenApiDocsTest : IntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `문서는 로그인 없이 볼 수 있다`() {
        mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$.info.title") { value("Kassa API") }
            jsonPath("$.paths['/api/auth/login'].post.summary") { value("로그인") }
            jsonPath("$.paths['/api/products'].get") { exists() }
        }
    }
}
