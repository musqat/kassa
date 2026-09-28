package com.kassa.payment.service

import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import com.kassa.order.domain.OrderStatus
import com.kassa.order.repository.OrderRepository
import com.kassa.order.service.OrderService
import com.kassa.payment.domain.PaymentStatus
import com.kassa.payment.gateway.PaymentGateway
import com.kassa.payment.repository.PaymentRepository
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

private val log = LoggerFactory.getLogger(CancelOrderService::class.java)

// 사용자가 누르는 취소. 결제 전이면 주문만 접고, 결제된 뒤면 대행사 취소까지 한다
// 트랜잭션을 걸지 않는다. 대행사 응답을 기다리는 동안 DB 커넥션을 쥐게 된다
@Service
class CancelOrderService(
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val orderService: OrderService,
    private val gateway: PaymentGateway,
    private val clock: Clock,
) {

    fun cancel(userId: Long, orderNo: String) {
        val order = orderRepository.findByOrderNoAndUserId(orderNo, userId)
            ?: throw BusinessException(ErrorCode.ORDER_NOT_FOUND)

        // 돈이 오간 주문이면 먼저 되돌린다. 취소가 실패하면 주문도 그대로 둔다
        if (order.status == OrderStatus.PAID) {
            refund(order.id!!, orderNo)
        }

        orderService.closeCanceled(userId, orderNo)
    }

    // 승인의 반대. 사가 보상과 같은 일을 사용자 요청으로 한다
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun refund(orderId: Long, orderNo: String) {
        val payment = paymentRepository.findByOrderIdAndStatus(orderId, PaymentStatus.PAID)
        if (payment == null) {
            log.warn("취소할 결제가 없다: {}", orderNo)
            return
        }

        // 호출 전에 저장해야 재시도에 같은 키를 보낸다
        val cancelKey = payment.startCancel(UUID.randomUUID().toString())
        paymentRepository.save(payment)

        gateway.cancelPayment(orderNo, payment.amount, cancelKey)

        payment.cancel(Instant.now(clock))
        paymentRepository.save(payment)
    }
}
