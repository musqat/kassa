package com.kassa.saga

import com.kassa.order.repository.OrderRepository
import com.kassa.saga.domain.SagaStatus
import java.time.Clock
import java.time.Duration
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private val log = LoggerFactory.getLogger(SagaRecoveryService::class.java)

// 요청이 끊겨 멈춘 사가를 이어서 끝낸다
@Service
class SagaRecoveryService(
    private val recorder: SagaRecorder,
    private val orderRepository: OrderRepository,
    private val orchestrator: OrderSagaOrchestrator,
    private val clock: Clock,
) {

    // 멈춘 사가를 집어 다시 돌린다. 리턴은 집은 건수
    // 승인 전에 끊긴 사가는 결제창 토큰이 없어 다시 승인을 못 걸고, 만료 뒤 정리된다
    fun recoverStuck(stuckAfter: Duration): Int {
        val cutoff = Instant.now(clock).minus(stuckAfter)
        val stuck = recorder.claimStuck(SagaStatus.RUNNING, cutoff, RECOVER_BATCH)

        for (instance in stuck) {
            val order = orderRepository.findByOrderNo(instance.orderNo) ?: continue
            try {
                orchestrator.start(instance.orderNo, order.userId, null)
            } catch (e: Exception){
                log.warn("사가 복구 실패: {}", instance.orderNo, e)
            }
        }
        return stuck.size
    }

    companion object {
        const val RECOVER_BATCH = 20
    }
}
