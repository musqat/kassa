package com.kassa.payment.service

import com.kassa.payment.domain.Payment
import com.kassa.payment.gateway.GatewayStatus
import com.kassa.payment.gateway.PaymentGateway
import com.kassa.payment.repository.PaymentRepository
import com.kassa.order.repository.OrderRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private val log = LoggerFactory.getLogger(PaymentResultChecker::class.java)

// 타임아웃으로 결과를 모르는 결제를 대행사에 물어 확정한다
// 주문 상태는 건드리지 않는다. 그 뒤는 사가 복구가 맡는다
@Service
class PaymentResultChecker(
    private val recorder: PaymentResultRecorder,
    private val paymentRepository: PaymentRepository,
    private val orderRepository: OrderRepository,
    private val gateway: PaymentGateway,
    private val clock: Clock,
) {

    // 결과를 모르는 결제를 한 번에 CHECK_BATCH 건까지 확정. 리턴은 확정한 건수
    fun checkStale(staleAfter: Duration): Int {
        val cutoff = Instant.now(clock).minus(staleAfter)
        val stale = recorder.claimStale(cutoff, CHECK_BATCH)
        var settled = 0

        for (payment in stale) {
            if (checkOne(payment)) {
                settled++
            }
        }

        return settled
    }

    private fun checkOne(payment: Payment): Boolean {
        val order = orderRepository.findById(payment.orderId).orElse(null)
        if (order == null) {
            log.warn("결제에 딸린 주문 없음: {}", payment.id)
            return false
        }

        val result = runCatching { gateway.getPayment(order.orderNo) }.getOrElse {
            log.warn("결제 결과 조회 실패: {}", order.orderNo, it)
            return false
        }

        if (result == null || result.status != GatewayStatus.PAID) {
            recorder.fail(payment, "대행사에 승인 기록 없음")
            return true
        }

        recorder.approve(payment, result)
        return true
    }


    companion object {
        const val CHECK_BATCH = 20
    }
}
