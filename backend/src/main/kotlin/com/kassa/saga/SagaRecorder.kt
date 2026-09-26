package com.kassa.saga

import com.kassa.saga.domain.SagaInstance
import com.kassa.saga.domain.SagaStatus
import com.kassa.saga.domain.SagaStep
import com.kassa.saga.repository.SagaInstanceRepository
import com.kassa.saga.repository.SagaStepRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.springframework.data.domain.Limit
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

// 사가 진행 상황 저장
// 메서드마다 자기 트랜잭션으로 커밋한다. 바깥이 나중에 실패해도 저장한 것은 남는다
@Component
class SagaRecorder(
    private val instanceRepository: SagaInstanceRepository,
    private val stepRepository: SagaStepRepository,
    private val clock: Clock,
) {

    // 사가 확보. 주문번호당 하나
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun startOrFind(orderNo: String): SagaInstance {
        val found  = instanceRepository.findByOrderNo(orderNo)
        if (found  != null){
            return found
        }

        return instanceRepository.save(SagaInstance(SAGA_TYPE, orderNo))
    }

    // SagaStep 실행 직전에 시도 횟수와 멱등키 저장
    // 같은 이름으로 다시 시작해도 행은 하나다. 멱등키는 처음 값을 지킨다
    // 대행사를 부른 뒤에 저장하면 그 사이에 죽었을 때 키를 잃는다
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun beginStep(instance: SagaInstance, stepName: String): SagaStep {
        val now = Instant.now(clock)
        val step = stepRepository.findBySagaInstanceIdAndStepName(instance.id!!, stepName)
            ?: SagaStep(instance.id!!, stepName)

        step.begin(now)
        step.claimKey(UUID.randomUUID().toString())
        instance.enterStep(stepName, now)

        instanceRepository.save(instance)
        return stepRepository.save(step)
    }

    // SagaStep 성공 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun stepDone(step: SagaStep, payload: String?) {
        step.done(payload)
        stepRepository.save(step)
    }

    // SagaStep 실패 저장. 사유 포함
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun stepFailed(step: SagaStep, reason: String) {
        step.fail(reason)
        stepRepository.save(step)
    }

    // 사가 완료 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun complete(instance: SagaInstance) {
        instance.complete(Instant.now(clock))
        instanceRepository.save(instance)
    }

    // 보상 시작 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun startCompensating(instance: SagaInstance) {
        instance.startCompensating(Instant.now(clock))
        instanceRepository.save(instance)
    }

    // SagaStep 보상 완료 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun stepCompensated(step: SagaStep) {
        step.compensated()
        stepRepository.save(step)
    }

    // 보상까지 끝난 사가 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun compensated(instance: SagaInstance) {
        instance.compensated(Instant.now(clock))
        instanceRepository.save(instance)
    }

    // 보상이 거듭 실패해 멈춘 사가 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun needsAttention(instance: SagaInstance) {
        instance.needsAttention(Instant.now(clock))
        instanceRepository.save(instance)
    }

    // 멈춘 사가를 집는다. 잠근 채로 갱신 시각을 밀어 다음 주기에 다시 걸리지 않게 한다
    // 짧은 트랜잭션이다. 대행사 호출은 이 밖에서 한다
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun claimStuck(status: SagaStatus, cutoff: Instant, limit: Int): List<SagaInstance> {
        val now = Instant.now(clock)
        val stuck = instanceRepository.findStuck(status, cutoff, Limit.of(limit))

        stuck.forEach { it.touch(now) }
        return instanceRepository.saveAll(stuck)
    }

    // 지나온 SagaStep 을 실행 순서대로 조회. 보상은 역순으로 처리
    @Transactional(readOnly = true)
    fun stepsOf(instanceId: Long): List<SagaStep> =
        stepRepository.findAllBySagaInstanceIdOrderByIdAsc(instanceId)

    companion object {
        const val SAGA_TYPE = "ORDER_PAYMENT"
    }
}
