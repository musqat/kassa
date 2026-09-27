package com.kassa.payment.service

import com.kassa.payment.domain.Payment
import com.kassa.payment.domain.PaymentStatus
import com.kassa.payment.gateway.GatewayPayment
import com.kassa.payment.repository.PaymentRepository
import java.time.Clock
import java.time.Instant
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

// 결제 확정 결과를 저장한다. 메서드마다 자기 트랜잭션으로 커밋한다
@Component
class PaymentResultRecorder(
    private val paymentRepository: PaymentRepository,
    private val clock: Clock,
) {

    // 결과를 모르는 결제 조회. 대행사 호출은 이 트랜잭션 밖에서 한다
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun claimStale(cutoff: Instant, limit: Int): List<Payment> =
        paymentRepository.findStale(PaymentStatus.REQUESTED, cutoff, Limit.of(limit))

    // 대행사가 승인한 것으로 확정
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun approve(payment: Payment, result: GatewayPayment) {
        payment.approve(result.transactionId, result.method, Instant.now(clock))
        paymentRepository.save(payment)
    }

    // 대행사가 승인하지 않은 것으로 확정
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun fail(payment: Payment, reason: String) {
        payment.fail(reason)
        paymentRepository.save(payment)
    }
}
