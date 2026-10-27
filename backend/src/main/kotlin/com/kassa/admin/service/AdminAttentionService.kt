package com.kassa.admin.service

import com.kassa.admin.dto.AdminAttentionResponse
import com.kassa.admin.dto.AttentionReason
import com.kassa.order.repository.OrderRepository
import com.kassa.payment.domain.PaymentStatus
import com.kassa.payment.repository.PaymentRepository
import com.kassa.saga.domain.SagaStatus
import com.kassa.saga.repository.SagaInstanceRepository
import java.time.Clock
import java.time.Duration
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 스케줄러가 끝내지 못해 사람이 봐야 하는 주문 목록
@Service
class AdminAttentionService(
    private val paymentRepository: PaymentRepository,
    private val instanceRepository: SagaInstanceRepository,
    private val orderRepository: OrderRepository,
    private val clock: Clock,
) {

    // 오래된 순
    @Transactional(readOnly = true)
    fun findAll(): List<AdminAttentionResponse> {
        val cutoff = clock.instant().minus(PAYMENT_UNKNOWN_AFTER)
        val payments = paymentRepository.findAllByStatusAndRequestedAtBefore(PaymentStatus.REQUESTED, cutoff)
        val orderNos = orderRepository.findAllById(payments.map { it.orderId }).associate { it.id!! to it.orderNo }

        val unknown = payments.mapNotNull { payment ->
            orderNos[payment.orderId]?.let { AdminAttentionResponse(it, AttentionReason.PAYMENT_UNKNOWN, payment.requestedAt) }
        }
        val stopped = instanceRepository.findAllByStatus(SagaStatus.NEEDS_ATTENTION)
            .map { AdminAttentionResponse(it.orderNo, AttentionReason.SAGA_STOPPED, it.updatedAt) }

        return (unknown + stopped).sortedBy { it.since }
    }

    companion object {
        // 결과 확인 주기(1분)를 여러 번 넘긴 승인 요청
        val PAYMENT_UNKNOWN_AFTER: Duration = Duration.ofMinutes(10)
    }
}
