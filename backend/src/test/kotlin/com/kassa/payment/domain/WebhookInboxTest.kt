package com.kassa.payment.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class WebhookInboxTest {

    private val now: Instant = Instant.parse("2026-09-26T10:00:00Z")
    private val later: Instant = now.plus(Duration.ofMinutes(1))

    private fun received() = WebhookInbox(
        eventId = "evt-1",
        payload = """{"paymentId":"20260926-ABCD1234"}""",
        headers = """{"webhook-id":"evt-1"}""",
        status = InboxStatus.RECEIVED,
    )

    @Test
    fun `서명 검증에 실패한 건은 FAILED 로 들어온다`() {
        val inbox = WebhookInbox(
            eventId = "evt-2",
            payload = "{}",
            headers = "{}",
            status = InboxStatus.FAILED,
        )

        assertThat(inbox.status).isEqualTo(InboxStatus.FAILED)
        assertThat(inbox.attemptCount).isEqualTo(0)
    }

    @Test
    fun `처리를 마치면 DONE 이 되고 처리 시각이 남는다`() {
        val inbox = received()

        inbox.done(later)

        assertThat(inbox.status).isEqualTo(InboxStatus.DONE)
        assertThat(inbox.processedAt).isEqualTo(later)
    }

    @Test
    fun `처리에 실패해도 RECEIVED 로 남는다`() {
        val inbox = received()

        inbox.retryLater("조회 실패")

        assertThat(inbox.status).isEqualTo(InboxStatus.RECEIVED)
        assertThat(inbox.attemptCount).isEqualTo(1)
        assertThat(inbox.error).isEqualTo("조회 실패")
    }

    @Test
    fun `실패가 쌓이면 시도 횟수가 오른다`() {
        val inbox = received()

        inbox.retryLater("조회 실패")
        inbox.retryLater("조회 실패")

        assertThat(inbox.status).isEqualTo(InboxStatus.RECEIVED)
        assertThat(inbox.attemptCount).isEqualTo(2)
        assertThat(inbox.error).isEqualTo("조회 실패")
    }

    @Test
    fun `포기하면 FAILED 가 되고 다시 처리되지 않는다`() {
        val inbox = received()

        inbox.giveUp("5회 초과", later)

        assertThat(inbox.status).isEqualTo(InboxStatus.FAILED)
        assertThat(inbox.processedAt).isEqualTo(later)
        assertThat(inbox.error).isEqualTo("5회 초과")
    }

    @Test
    fun `실패 사유가 길면 잘라서 담는다`() {
        val inbox = received()

        inbox.retryLater("긴".repeat(300))

        assertThat(inbox.error).hasSize(255)
    }

    @Test
    fun `재시도로 성공하면 앞선 실패 메시지가 지워진다`() {
        val inbox = received()
        inbox.retryLater("조회 실패")

        inbox.done(later)

        assertThat(inbox.status).isEqualTo(InboxStatus.DONE)
        assertThat(inbox.error).isNull()
        assertThat(inbox.attemptCount).isEqualTo(1)
    }
}
