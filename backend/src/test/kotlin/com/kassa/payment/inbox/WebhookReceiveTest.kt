package com.kassa.payment.inbox

import com.kassa.support.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Fail
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.post

@AutoConfigureMockMvc
class WebhookReceiveTest : IntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var inboxRepository: WebhookInboxRepository

    private val body = """{"paymentId":"20260928-ABCD1234","status":"DONE"}"""

    /** 서명이 "valid" 면 가짜 게이트웨이가 통과시킨다 */
    private fun send(eventId: String, signature: String = "valid"): ResultActionsDsl =
        mockMvc.post("/api/payments/webhook") {
            header("webhook-id", eventId)
            header("webhook-signature", signature)
            contentType = MediaType.APPLICATION_JSON
            content = body
        }

    @Test
    fun `서명이 맞으면 RECEIVED 로 저장된다`() {
        send("evt-1").andExpect { status { isOk() } }

        val saved = inboxRepository.findAll().single()
        assertThat(saved.eventId).isEqualTo("evt-1")
        assertThat(saved.status).isEqualTo(InboxStatus.RECEIVED)
        assertThat(saved.payload).isEqualTo(body)
        assertThat(saved.attemptCount).isEqualTo(0)
    }

    @Test
    fun `서명이 틀려도 200 이고 FAILED 로 저장된다`() {
        send("evt-2", signature = "forged").andExpect { status { isOk() } }
        val saved = inboxRepository.findAll().single()
        assertThat(saved.status).isEqualTo(InboxStatus.FAILED)
    }

    @Test
    fun `헤더 이름의 대소문자가 달라도 서명을 읽는다`() {
        // HTTP 헤더 이름은 대소문자를 가리지 않는다. 보내는 쪽이 첫 글자를 대문자로 보내도 같은 헤더다
        mockMvc.post("/api/payments/webhook") {
            header("Webhook-Id", "evt-case")
            header("Webhook-Signature", "valid")
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect { status { isOk() } }

        val saved = inboxRepository.findAll().single()
        assertThat(saved.eventId).isEqualTo("evt-case")
        assertThat(saved.status).isEqualTo(InboxStatus.RECEIVED)
    }

    @Test
    fun `같은 event_id 가 두 번 와도 행은 하나다`() {
        send("evt-3").andExpect { status { isOk() } }
        send("evt-3").andExpect { status { isOk() } }

        assertThat(inboxRepository.count()).isEqualTo(1)
    }

    @Test
    fun `webhook-id 헤더가 없으면 unknown 으로 저장된다`() {
        mockMvc.post("/api/payments/webhook") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect { status { isOk() } }

        val saved = inboxRepository.findAll().single()
        assertThat(saved.eventId).startsWith("unknown-")
        assertThat(saved.status).isEqualTo(InboxStatus.FAILED)
    }
}
