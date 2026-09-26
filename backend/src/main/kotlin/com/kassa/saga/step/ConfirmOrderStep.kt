package com.kassa.saga.step

import com.kassa.cart.repository.CartItemRepository
import com.kassa.catalog.repository.ProductRepository
import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import com.kassa.order.domain.OrderStatus
import com.kassa.order.repository.OrderRepository
import com.kassa.saga.SagaContext
import com.kassa.saga.SagaStepHandler
import java.time.Clock
import java.time.Instant
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

// 주문 확정. 재고를 확정 차감하고 장바구니를 비운다
@Component
class ConfirmOrderStep(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    private val cartItemRepository: CartItemRepository,
    private val clock: Clock,
) : SagaStepHandler {

    override val name = NAME

    // 외부 호출이 없어 한 트랜잭션으로 묶는다. 셋 중 하나가 실패하면 셋 다 되돌아간다
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun execute(context: SagaContext): String? {
        val now = Instant.now(clock)

        val order = orderRepository.findByOrderNoAndUserId(context.orderNo, context.userId)
            ?: throw BusinessException(ErrorCode.ORDER_NOT_FOUND)

        // 이 단계가 두 번 돌면 재고가 두 번 차감된다
        val alreadyPaid = order.status == OrderStatus.PAID
        order.markPaid(now)
        if (alreadyPaid) {
            return null
        }

        val productIds = order.items.map { it.productId }
        val locked = productRepository.findAllForUpdate(productIds).associateBy { it.id!! }
        order.items.forEach { locked.getValue(it.productId).confirmReservation(it.quantity) }

        cartItemRepository.deleteAllByUserId(context.userId)

        return null
    }

    companion object {
        const val NAME = "CONFIRM_ORDER"
    }
}
