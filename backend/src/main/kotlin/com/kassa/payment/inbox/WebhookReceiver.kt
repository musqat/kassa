package com.kassa.payment.inbox

import com.kassa.payment.gateway.PaymentGateway
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = LoggerFactory.getLogger(WebhookReceiver::class.java)

// 웹훅을 검증해 인박스에 저장한다. 처리는 워커가 따로 한다
@Service
class WebhookReceiver(
    private val gateway: PaymentGateway,
    private val inboxRepository: WebhookInboxRepository,
) {

    // 서명이 틀리면 FAILED 로 저장한다. 워커가 가져가지 않아 위조된 주문번호로 조회하지 않는다
    fun receive(headers: Map<String, String>, rawBody: String) {
        val eventId = gateway.verifyWebhook(headers, rawBody)

        if (eventId == null) {
            val failedId = headers["webhook-id"] ?: "unknown-${System.nanoTime()}"
            log.warn("웹훅 검증 실패 {}", failedId)
            return save(failedId, InboxStatus.FAILED, rawBody, headers)
        }

        return save(eventId, InboxStatus.RECEIVED, rawBody, headers)
    }

    // 같은 event_id 가 두 번 오면 유니크 제약에 걸린다. 재전송이라 그냥 넘긴다
    @Transactional
    fun save(eventId: String, status: InboxStatus, rawBody: String, headers: Map<String, String>) {
        try {
            inboxRepository.save(WebhookInbox(eventId, rawBody, headers.toString(), status))
        } catch (e: DataIntegrityViolationException) {
            log.warn("이미 들어온 웹훅 {}", eventId)
        }
    }
}
