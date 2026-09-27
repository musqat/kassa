package com.kassa.payment.inbox

import java.time.Clock
import java.time.Instant
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

// 인박스 상태를 저장한다. 메서드마다 자기 트랜잭션으로 커밋한다
@Component
class InboxRecorder(
    private val inboxRepository: WebhookInboxRepository,
    private val clock: Clock,
) {

    // 처리할 웹훅 조회. 대행사 호출은 이 트랜잭션 밖에서 한다
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun claimReceived(limit: Int): List<WebhookInbox> =
        inboxRepository.findUnprocessed(InboxStatus.RECEIVED, Limit.of(limit))

    // 처리 완료 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun done(inbox: WebhookInbox) {
        inbox.done(Instant.now(clock))
        inboxRepository.save(inbox)
    }

    // 실패 저장. 시도 횟수가 한계를 넘으면 더 처리하지 않는다
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun retry(inbox: WebhookInbox, reason: String, maxAttempts: Int) {
        inbox.retryLater(reason)
        if (inbox.attemptCount >= maxAttempts) {
            inbox.giveUp(reason, Instant.now(clock))
        }

        inboxRepository.save(inbox)
    }

    // 재시도해도 같은 결과인 경우. 바로 중단한다
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun giveUp(inbox: WebhookInbox, reason: String) {
        inbox.giveUp(reason, Instant.now(clock))
        inboxRepository.save(inbox)
    }
}
