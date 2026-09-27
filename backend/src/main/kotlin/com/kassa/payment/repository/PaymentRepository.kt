package com.kassa.payment.repository

import com.kassa.payment.domain.Payment
import com.kassa.payment.domain.PaymentStatus
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import java.time.Instant
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param

interface PaymentRepository : JpaRepository<Payment, Long> {

    // 한 주문에 결제 시도가 여럿. 최신부터
    fun findAllByOrderIdOrderByIdDesc(orderId: Long): List<Payment>

    fun findByOrderIdAndStatus(orderId: Long, status: PaymentStatus): Payment?

    // 대행사 기록과 대조할 때는 그날 승인된 결제만 본다
    fun findAllByStatusAndApprovedAtBetween(
        status: PaymentStatus,
        from: Instant,
        to: Instant,
    ): List<Payment>

    // 결과를 모르는 결제. 다른 워커가 잡은 행은 건너뛴다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select p from Payment p where p.status = :status and p.requestedAt < :cutoff order by p.id")
    fun findStale(
        @Param("status") status: PaymentStatus,
        @Param("cutoff") cutoff: Instant,
        limit: Limit,
    ): List<Payment>
}
