package com.kassa.payment.reconcile

import java.time.Clock
import java.time.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(ReconcileScheduler::class.java)

// 테스트에서는 app.payment.reconcile.enabled 를 false 로 두고 서비스를 직접 부른다
@Component
@ConditionalOnProperty(
    name = ["app.payment.reconcile.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class ReconcileScheduler(
    private val reconcileService: ReconcileService,
    private val clock: Clock,
) {

    // 전날 하루치를 대조한다. 새벽 4시 30분인 이유는 웹훅 재전송이 5.7시간까지 이어져서다
    // 그 전에 돌리면 아직 오는 중인 웹훅을 불일치로 잡는다
    @Scheduled(cron = "\${app.payment.reconcile.cron}", zone = "Asia/Seoul")
    fun reconcileYesterday() {
        val targetDate = LocalDate.now(clock.withZone(ReconcileService.KST)).minusDays(1)
        val found = reconcileService.reconcile(targetDate)

        if (found > 0) log.warn("결제 기록 불일치 {}건: {}", found, targetDate)
    }
}
