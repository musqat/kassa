package com.kassa.saga.step

import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import com.kassa.order.repository.OrderRepository
import com.kassa.payment.domain.Payment
import com.kassa.payment.domain.PaymentStatus
import com.kassa.payment.gateway.AlreadyPaidException
import com.kassa.payment.gateway.GatewayException
import com.kassa.payment.gateway.GatewayPayment
import com.kassa.payment.gateway.PaymentGateway
import com.kassa.payment.repository.PaymentRepository
import com.kassa.saga.SagaContext
import com.kassa.saga.SagaStepHandler
import java.time.Instant
import java.time.Clock
import java.util.UUID
import org.springframework.stereotype.Component

// 대행사 승인과 취소
@Component
class ApprovePaymentStep(
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val gateway: PaymentGateway,
    private val clock: Clock,
) : SagaStepHandler {

    override val name = NAME

    // 승인 결과를 Payment 에 저장
    // 트랜잭션을 걸지 않는다. 대행사 응답을 기다리는 동안 DB 커넥션을 쥐게 된다
    override fun execute(context: SagaContext): String? {
        val now = Instant.now(clock)

        val order = orderRepository.findByOrderNoAndUserId(context.orderNo, context.userId)
            ?: throw BusinessException(ErrorCode.ORDER_NOT_FOUND)

        // 확인 스케줄러가 PAID 로 확정해 둔 결제가 있으면 그걸 쓴다
        // 새로 만들면 한 주문에 PAID 가 둘이 되어 부분 유니크 인덱스에 걸린다
        val settled = paymentRepository.findByOrderIdAndStatus(order.id!!, PaymentStatus.PAID)
        if (settled != null) {
            return settled.id.toString()
        }

        // 재시도면 앞서 만든 행을 다시 쓴다. 매번 만들면 REQUESTED 가 쌓인다
        val payment = paymentRepository.findByOrderIdAndStatus(order.id!!, PaymentStatus.REQUESTED)
            ?: paymentRepository.save(Payment(order.id!!, order.totalAmount))

        // 이미 승인된 건은 성공으로 친다. 실패로 보면 멀쩡한 결제를 보상으로 취소한다
        // 타임아웃은 잡지 않는다. REQUESTED 로 남아야 확인 스케줄러가 가져간다
        val result: GatewayPayment = try {
            gateway.confirmPayment(
                context.orderNo,
                order.totalAmount,
                context.idempotencyKey(),
                context.approvalToken,
            )
        } catch (e: AlreadyPaidException) {
            e.payment
        } catch (e: GatewayException) {
            payment.fail(e.message ?: "승인 거절")
            paymentRepository.save(payment)
            throw e
        }

        // 사전 등록으로 한 번 막았지만 응답으로 다시 확인한다
        if (result.amount != order.totalAmount) {
            payment.fail("금액 불일치: ${order.totalAmount} != ${result.amount}")
            paymentRepository.save(payment)
            throw BusinessException(ErrorCode.PAYMENT_AMOUNT_MISMATCH)
        }

        payment.approve(result.transactionId, result.method, now)
        paymentRepository.save(payment)

        return payment.id.toString()
    }

    // 대행사 취소를 부르고 Payment 를 CANCELED 로
    override fun compensate(context: SagaContext) {
        val now = Instant.now(clock)

        val paymentId = context.payloadOf(NAME) ?: return
        val payment = paymentRepository.findById(paymentId.toLong()).orElse(null)
        if (payment == null || payment.status != PaymentStatus.PAID) {
            return
        }

        // 호출 전에 저장해야 재시도에 같은 키를 보낸다
        val cancelKey = payment.startCancel(UUID.randomUUID().toString())
        paymentRepository.save(payment)

        gateway.cancelPayment(context.orderNo, payment.amount, cancelKey)

        payment.cancel(now)
        paymentRepository.save(payment)
    }

    companion object {
        const val NAME = "APPROVE_PAYMENT"
    }
}
