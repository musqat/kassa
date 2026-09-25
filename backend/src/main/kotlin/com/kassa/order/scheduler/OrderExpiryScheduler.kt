package com.kassa.order.scheduler

import com.kassa.order.service.OrderService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration

private val log = LoggerFactory.getLogger(OrderExpiryScheduler::class.java)

// 테스트에서는 app.order.expiry.enabled 를 false 로 두고 서비스를 직접 부른다
@Component
@ConditionalOnProperty(
    name = ["app.order.expiry.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class OrderExpiryScheduler(
    private val orderService: OrderService,
    @Value("\${app.order.expiry.after}") private val expiry: Duration,
) {

    @Scheduled(fixedDelayString = "\${app.order.expiry.interval}")
    fun expire() {
        val count = orderService.expireOverdue(expiry)
        if (count > 0) log.info("결제되지 않은 주문 {}건 정리", count)
    }
}
