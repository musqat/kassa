package com.kassa.saga

// SagaStep 하나가 할 줄 아는 것 — 실행과 보상
// 보상(compensation)은 사가 패턴 용어다. DB 롤백이 안 되는 작업(대행사 승인)을
// 반대 작업(취소 호출)으로 되돌리는 것을 말한다
interface SagaStepHandler {

    /** saga_step.step_name 에 저장할 이름 */
    val name: String

    /** 실행. saga_step.payload 에 저장 */
    fun execute(context: SagaContext): String?

    /** 실행 되돌리기. 승인이면 취소를 부른다 */
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
