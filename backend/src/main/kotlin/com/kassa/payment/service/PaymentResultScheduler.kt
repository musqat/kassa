package com.kassa.payment.service

import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(PaymentResultScheduler::class.java)

// 테스트에서는 app.payment.result-check.enabled 를 false 로 두고 서비스를 직접 부른다
@Component
@ConditionalOnProperty(
    name = ["app.payment.result-check.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class PaymentResultScheduler(
    private val checker: PaymentResultChecker,
    @Value("\${app.payment.result-check.stale-after}") private val staleAfter: Duration,
) {

    @Scheduled(fixedDelayString = "\${app.payment.result-check.interval}")
    fun check() {
        val count = checker.checkStale(staleAfter)
        if (count > 0) log.info("결과를 모르던 결제 {}건 확정", count)
    }
}
