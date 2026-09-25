package com.kassa.payment.repository

import com.kassa.payment.domain.PaymentLog
import org.springframework.data.jpa.repository.JpaRepository

interface PaymentLogRepository : JpaRepository<PaymentLog, Long> {

    fun findAllByPaymentIdOrderByIdAsc(paymentId: Long): List<PaymentLog>
}
