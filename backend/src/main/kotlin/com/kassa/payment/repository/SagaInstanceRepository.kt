package com.kassa.payment.repository

import com.kassa.payment.domain.SagaInstance
import com.kassa.payment.domain.SagaStatus
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import java.time.Instant
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param

interface SagaInstanceRepository : JpaRepository<SagaInstance, Long> {

    fun findByOrderNo(orderNo: String): SagaInstance?

    // 만료 스케줄러의 제외 조건
    fun existsByOrderNo(orderNo: String): Boolean

    // 멈춘 사가. 다른 워커가 잡은 행은 건너뛴다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select s from SagaInstance s where s.status = :status and s.updatedAt < :cutoff order by s.id")
    fun findStuck(
        @Param("status") status: SagaStatus,
        @Param("cutoff") cutoff: Instant,
        limit: Limit,
    ): List<SagaInstance>
}
