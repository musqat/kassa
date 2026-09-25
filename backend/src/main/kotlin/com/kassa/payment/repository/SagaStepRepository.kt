package com.kassa.payment.repository

import com.kassa.payment.domain.SagaStep
import org.springframework.data.jpa.repository.JpaRepository

interface SagaStepRepository : JpaRepository<SagaStep, Long> {

    // 실행한 순서. 되돌릴 때 사용
    fun findAllBySagaInstanceIdOrderByIdAsc(sagaInstanceId: Long): List<SagaStep>

    fun findBySagaInstanceIdAndStepName(sagaInstanceId: Long, stepName: String): SagaStep?
}
