package com.kassa.payment.reconcile

import java.time.LocalDate
import org.springframework.data.jpa.repository.JpaRepository

interface ReconcileDiffRepository : JpaRepository<ReconcileDiff, Long> {

    fun findAllByTargetDateOrderByIdAsc(targetDate: LocalDate): List<ReconcileDiff>

    // 같은 날짜를 다시 대조할 때 이미 남긴 건을 거른다
    fun existsByTargetDateAndOrderNoAndKind(
        targetDate: LocalDate,
        orderNo: String?,
        kind: DiffKind,
    ): Boolean
}
