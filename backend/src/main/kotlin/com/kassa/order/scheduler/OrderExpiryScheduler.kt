package com.kassa.order.scheduler

import com.kassa.order.service.OrderService
import com.kassa.saga.repository.SagaInstanceRepository
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
    private val sagaInstanceRepository: SagaInstanceRepository,
    @Value("\${app.order.expiry.after}") private val expiry: Duration,
) {

    @Scheduled(fixedDelayString = "\${app.order.expiry.interval}")
    fun expire() {
        // 사가가 시작된 주문은 사가 복구가 맡는다. 결제창에 오래 머물다 승인을 걸 수 있다
        val count = orderService.expireOverdue(expiry) { sagaInstanceRepository.existsByOrderNo(it) }
        if (count > 0) log.info("결제되지 않은 주문 {}건 정리", count)
    }
}
