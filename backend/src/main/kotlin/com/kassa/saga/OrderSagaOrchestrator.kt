package com.kassa.saga

import com.kassa.payment.gateway.GatewayTimeoutException
import com.kassa.saga.domain.SagaInstance
import com.kassa.saga.domain.SagaStatus
import com.kassa.saga.domain.StepStatus
import com.kassa.saga.domain.SagaStep
import com.kassa.saga.step.ApprovePaymentStep
import com.kassa.saga.step.ConfirmOrderStep
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(OrderSagaOrchestrator::class.java)

// SagaStep 을 순서대로 실행하고 결과를 SagaRecorder 에 남긴다
@Component
class OrderSagaOrchestrator(
    private val recorder: SagaRecorder,
    approvePayment: ApprovePaymentStep,
    confirmOrder: ConfirmOrderStep,
) {

    // 실행 순서. 스프링이 주입하는 목록은 순서를 보장하지 않아 직접 세운다
    private val steps: List<SagaStepHandler> = listOf(approvePayment, confirmOrder)

    // 승인부터 주문 확정까지
    fun start(orderNo: String, userId: Long, approvalToken: String?) {
        val instance = recorder.startOrFind(orderNo)

        // 끝난 사가는 다시 돌지 않는다. SagaStep 을 또 실행하면 결제가 두 건 된다
        if (instance.status == SagaStatus.COMPLETED) {
            return
        }

        for (handler in steps) {
            val step = recorder.beginStep(instance, handler.name)
            val context = contextFor(orderNo, userId, approvalToken, instance, step)

            try {
                val payload = handler.execute(context)
                recorder.stepDone(step, payload)
            } catch (e: Exception) {
                recorder.stepFailed(step, e.message ?: e.javaClass.simpleName)
                log.warn("사가 단계 실패: {} {}", orderNo, handler.name, e)

                // 타임아웃이면 보상하지 않고 RUNNING 으로 둔다. 승인 여부를 모르는데 되돌리면
                // 멀쩡한 결제를 취소한다. 확인 스케줄러가 결과를 확정한 뒤 복구가 이어받는다
                if (e is GatewayTimeoutException) throw e

                compensate(orderNo, userId, approvalToken, instance)
                throw e
            }
        }

        recorder.complete(instance)
    }

    // 마친 SagaStep 을 역순으로 되돌린다(보상). 나중 단계가 앞 단계 결과에 기대고 있어서다
    // 하나라도 못 되돌리면 뒤는 손대지 않고 NEEDS_ATTENTION 으로 멈춘다
    // 예외를 밖으로 던지지 않는다. 부른 쪽이 원래 예외를 던진다
    private fun compensate(
        orderNo: String,
        userId: Long,
        approvalToken: String?,
        instance: SagaInstance,
    ) {
        recorder.startCompensating(instance)
        val doneSteps = recorder.stepsOf(instance.id!!).filter { it.status == StepStatus.DONE }
        for (step in doneSteps.reversed()) {
            val handler = steps.first { it.name == step.stepName }
            val context = contextFor(orderNo, userId, approvalToken, instance, step)
            try {
                handler.compensate(context)
                recorder.stepCompensated(step)
            } catch (e: Exception) {
                log.warn("보상 실패: {} {}", orderNo, step.stepName, e)
                recorder.needsAttention(instance)
                return
            }
        }
        recorder.compensated(instance)
    }

    // SagaStep 이 멱등키와 앞선 payload 를 읽는 통로
    private fun contextFor(
        orderNo: String,
        userId: Long,
        approvalToken: String?,
        instance: SagaInstance,
        step: SagaStep,
    ) = SagaContext(
        orderNo = orderNo,
        userId = userId,
        approvalToken = approvalToken,
        // beginStep 에서 잡아 저장한 키다. 없을 수 없다
        idempotencyKey = { step.idempotencyKey!! },
        payloadOf = { name ->
            recorder.stepsOf(instance.id!!)
                .firstOrNull { it.stepName == name }
                ?.payload
        },
    )
}
