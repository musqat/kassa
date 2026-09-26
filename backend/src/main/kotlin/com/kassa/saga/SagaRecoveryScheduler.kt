package com.kassa.saga

import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(SagaRecoveryScheduler::class.java)

// 테스트에서는 app.saga.recovery.enabled 를 false 로 두고 서비스를 직접 부른다
@Component
@ConditionalOnProperty(
    name = ["app.saga.recovery.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class SagaRecoveryScheduler(
    private val recoveryService: SagaRecoveryService,
    @Value("\${app.saga.recovery.stuck-after}") private val stuckAfter: Duration,
) {

    @Scheduled(fixedDelayString = "\${app.saga.recovery.interval}")
    fun recover() {
        val count = recoveryService.recoverStuck(stuckAfter)
        if (count > 0) log.info("멈춘 사가 {}건 정리", count)
    }
}
