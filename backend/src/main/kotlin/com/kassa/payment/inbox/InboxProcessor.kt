package com.kassa.payment.inbox

import com.kassa.order.repository.OrderRepository
import com.kassa.payment.gateway.GatewayStatus
import com.kassa.payment.gateway.PaymentGateway
import com.kassa.saga.OrderSagaOrchestrator
import java.time.Clock
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

private val log = LoggerFactory.getLogger(InboxProcessor::class.java)

// 쌓인 웹훅을 한 건씩 처리한다
@Service
class InboxProcessor(
    private val recorder: InboxRecorder,
    private val orderRepository: OrderRepository,
    private val orchestrator: OrderSagaOrchestrator,
    private val gateway: PaymentGateway,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
    @Value("\${app.payment.inbox.max-attempts}") private val maxAttempts: Int,
) {

    // 쌓인 웹훅을 한 번에 BATCH 건까지 처리. 리턴은 가져온 건수
    fun processPending(): Int {
        val pending = recorder.claimReceived(BATCH)

        for (inbox in pending){
            processOne(inbox)
        }

        return pending.size
    }

    // 본문에서 주문번호만 꺼내고 결제 상태는 대행사에 다시 묻는다. 승인 전이면 RECEIVED 로 둔다
    // 주문 상태는 직접 바꾸지 않고 사가를 부른다. 승인과 같은 길을 지나야 재고·장바구니가 맞는다
    private fun processOne(inbox: WebhookInbox) {
        // 본문의 paymentId 가 주문번호다. 없거나 깨졌으면 null
        val orderNo = runCatching { objectMapper.readTree(inbox.payload).get("paymentId")?.asString() }
            .getOrNull()
        if (orderNo == null){
            recorder.giveUp(inbox, "주문번호 없음")
            return
        }

        val gatewayPayment = gateway.getPayment(orderNo)
        if (gatewayPayment == null || gatewayPayment.status != GatewayStatus.PAID){
            recorder.retry(inbox, "대행사에 승인된 결제 없음", maxAttempts)
            return
        }

        val order = orderRepository.findByOrderNo(orderNo)
        if (order == null){
            recorder.giveUp(inbox, "주문 없음")
            return
        }

        try {
            orchestrator.start(orderNo, order.userId, null)
            recorder.done(inbox)
        }catch (e : Exception){
            log.warn("웹훅 처리 실패 : {}", orderNo, e)
            recorder.retry(inbox, e.message ?: e.javaClass.simpleName, maxAttempts)
        }

    }

    companion object {
        const val BATCH = 20
    }
}
