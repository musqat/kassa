package com.kassa.admin.service

import com.kassa.admin.dto.AdminOrderDetailResponse
import com.kassa.admin.dto.AdminOrderResponse
import com.kassa.common.crypto.PhoneCipher
import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import com.kassa.order.domain.OrderStatus
import com.kassa.order.repository.OrderRepository
import com.kassa.payment.repository.PaymentRepository
import com.kassa.user.repository.UserRepository
import java.time.Clock
import java.time.Instant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 관리자 주문 관리. 회원을 가리지 않고 본다
@Service
class AdminOrderService(
    private val orderRepository: OrderRepository,
    private val userRepository: UserRepository,
    private val paymentRepository: PaymentRepository,
    private val phoneCipher: PhoneCipher,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun findAll(status: OrderStatus?): List<AdminOrderResponse> {
        val orders = if (status == null) {
            orderRepository.findAllByOrderByIdDesc()
        } else {
            orderRepository.findAllByStatusOrderByIdDesc(status)
        }

        val loginIds = userRepository.findAllById(orders.map { it.userId })
            .associate { it.id!! to it.loginId }

        return orders.map { AdminOrderResponse.of(it, loginIds[it.userId]) }
    }

    @Transactional(readOnly = true)
    fun find(orderNo: String): AdminOrderDetailResponse {
        val order = orderRepository.findByOrderNo(orderNo)
            ?: throw BusinessException(ErrorCode.ORDER_NOT_FOUND)
        val loginId = userRepository.findById(order.userId).orElse(null)?.loginId
        val method = paymentRepository.findAllByOrderIdOrderByIdDesc(order.id!!)
            .firstOrNull { it.method != null }
            ?.method

        return AdminOrderDetailResponse.of(
            order = order,
            loginId = loginId,
            // 배송하려면 전체 번호가 있어야 한다. 목록에는 번호를 주지 않는다
            phone = phoneCipher.decrypt(order.phoneEnc),
            method = method,
        )
    }

    @Transactional
    fun markShipped(orderNo: String): AdminOrderResponse {
        val order = orderRepository.findByOrderNo(orderNo)
            ?: throw BusinessException(ErrorCode.ORDER_NOT_FOUND)
        order.markShipped(Instant.now(clock))

        val loginId = userRepository.findById(order.userId).orElse(null)?.loginId

        return AdminOrderResponse.of(order, loginId)
    }
}
