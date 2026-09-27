package com.kassa.payment.inbox

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(InboxWorker::class.java)

// 테스트에서는 app.payment.inbox.enabled 를 false 로 두고 서비스를 직접 부른다
@Component
@ConditionalOnProperty(
    name = ["app.payment.inbox.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class InboxWorker(private val processor: InboxProcessor) {

    @Scheduled(fixedDelayString = "\${app.payment.inbox.interval}")
    fun process() {
        val count = processor.processPending()
        if (count > 0) log.info("웹훅 {}건 처리", count)
    }
}
