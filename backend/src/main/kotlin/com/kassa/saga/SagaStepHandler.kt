package com.kassa.saga

// SagaStep 실행과 보상
interface SagaStepHandler {

    /** saga_step.step_name 에 저장할 이름 */
    val name: String

    /** 실행. saga_step.payload 에 저장 */
    fun execute(context: SagaContext): String?

    /** 실행 되돌리기 */
    fun compensate(context: SagaContext) {}
}

data class SagaContext(
    val orderNo: String,
    val userId: Long,
    val approvalToken: String?,
    // 지금 SagaStep 의 멱등키
    val idempotencyKey: () -> String,
    // 앞선 SagaStep 이 저장한 payload
    val payloadOf: (String) -> String?,
)
