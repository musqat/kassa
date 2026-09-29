package com.kassa.order.service

import com.kassa.cart.repository.CartItemRepository
import com.kassa.catalog.domain.ProductStatus
import com.kassa.catalog.repository.ProductRepository
import com.kassa.common.crypto.PhoneCipher
import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import com.kassa.order.domain.Address
import com.kassa.order.domain.Order
import com.kassa.order.domain.OrderItem
import com.kassa.order.domain.OrderNoGenerator
import com.kassa.order.domain.OrderStatus
import com.kassa.order.domain.ShippingInfo
import com.kassa.order.dto.OrderResponse
import com.kassa.order.dto.PlaceOrderRequest
import com.kassa.order.dto.PlaceOrderResponse
import com.kassa.order.repository.AddressRepository
import com.kassa.order.repository.OrderRepository
import com.kassa.payment.repository.PaymentRepository
import com.kassa.pricing.ShippingPolicy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.data.domain.Limit
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val addressRepository: AddressRepository,
    private val cartItemRepository: CartItemRepository,
    private val productRepository: ProductRepository,
    private val paymentRepository: PaymentRepository,
    private val orderNoGenerator: OrderNoGenerator,
    private val phoneCipher: PhoneCipher,
    private val clock: Clock,
) {

    // 장바구니를 주문으로 옮기고 재고를 선점한다. 장바구니는 결제가 끝난 뒤에 비운다
    @Transactional
    fun place(userId: Long, request: PlaceOrderRequest): PlaceOrderResponse {
        val now = Instant.now(clock)
        val cartItems = cartItemRepository.findAllByUserId(userId)
        if (cartItems.isEmpty()) {
            throw BusinessException(ErrorCode.EMPTY_ORDER)
        }

        // 잠근 뒤의 상태로 판단한다. 먼저 읽어 두면 그 값이 영속성 컨텍스트에 남아 잠금이 헛돈다
        val locked = productRepository.findAllForUpdate(cartItems.map { it.product.id!! })
            .associateBy { it.id!! }

        val lines = cartItems.filter { locked.getValue(it.product.id!!).status == ProductStatus.ON_SALE }
        if (lines.isEmpty()) {
            throw BusinessException(ErrorCode.EMPTY_ORDER)
        }


        lines.forEach { line -> locked.getValue(line.product.id!!).reserve(line.quantity) }

        val itemAmount = lines.sumOf { locked.getValue(it.product.id!!).price * it.quantity }
        val shippingFee = ShippingPolicy.feeFor(itemAmount)

        // 전화번호는 암호화해서 담는다
        val shipping = ShippingInfo(
            receiver = request.receiver.trim(),
            phoneEnc = phoneCipher.encrypt(request.phone),
            zipcode = request.zipcode,
            addr1 = request.addr1.trim(),
            addr2 = request.addr2?.trim(),
        )

        val order = Order(nextOrderNo(now), userId, itemAmount, shippingFee, shipping)

        lines.forEach { line ->
            val product = locked.getValue(line.product.id!!)
            order.addItem(OrderItem.of(product, line.quantity))
        }

        orderRepository.save(order)

        if (request.saveAddress) {
            saveAddress(userId, shipping)
        }

        return PlaceOrderResponse(order.orderNo, order.totalAmount)

    }

    // 주문을 CANCELED 로 닫고 재고를 되돌린다. 대행사 취소는 부르는 쪽이 이미 끝냈다
    // PENDING 이었으면 선점만 풀고, PAID 였으면 stock 에서 빠진 수량을 되돌린다
    @Transactional
    fun closeCanceled(userId: Long, orderNo: String) {
        val now = Instant.now(clock)
        val order = orderRepository.findByOrderNoAndUserId(orderNo, userId)
            ?: throw BusinessException(ErrorCode.ORDER_NOT_FOUND)
        // cancel 뒤에는 CANCELED 라 원래 상태를 알 수 없다
        val wasPaid = order.status == OrderStatus.PAID
        order.cancel(now)

        val locked = productRepository.findAllForUpdate(order.items.map { it.productId })
            .associateBy { it.id!! }
        order.items.forEach { item ->
            val product = locked.getValue(item.productId)
            if (wasPaid) {
                product.restore(item.quantity)
            } else {
                product.releaseReservation(item.quantity)
            }
        }
    }

    // 결제 없이 기한이 지난 주문을 접는다. 한 번에 EXPIRE_BATCH 건씩
    // skip 이 true 인 주문번호는 건드리지 않는다
    @Transactional
    fun expireOverdue(expiry: Duration, skip: (String) -> Boolean = { false }): Int {
        val now = Instant.now(clock)
        val overdue = orderRepository.findExpired(OrderStatus.PENDING, now.minus(expiry), Limit.of(EXPIRE_BATCH))
        val expired = overdue.filterNot { skip(it.orderNo) }

        expired.forEach { order ->
            order.expire(now)
            releaseStock(order)
        }

        return expired.size
    }

    // 주문 줄마다 잡아 둔 수량을 되돌린다. 여기서도 상품 행을 잠근다
    private fun releaseStock(order: Order) {
        val locked = productRepository.findAllForUpdate(order.items.map { it.productId })
            .associateBy { it.id!! }

        order.items.forEach { item ->
            locked.getValue(item.productId).releaseReservation(item.quantity)
        }
    }

    @Transactional(readOnly = true)
    fun findMyOrders(userId: Long): List<OrderResponse> {
        return orderRepository.findAllByUserIdOrderByIdDesc(userId).map { toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun findMyOrder(userId: Long, orderNo: String): OrderResponse {
        val order = orderRepository.findByOrderNoAndUserId(orderNo, userId)
            ?: throw BusinessException(ErrorCode.ORDER_NOT_FOUND)

        // 상세에서만 결제 수단을 같이 읽는다. 목록에서 읽으면 주문마다 조회가 한 번씩 는다
        // 취소된 주문도 결제 수단을 보여주려고 상태를 가리지 않고 마지막 결제를 읽는다
        val method = paymentRepository.findAllByOrderIdOrderByIdDesc(order.id!!)
            .firstOrNull { it.method != null }
            ?.method

        return toResponse(order, method)
    }

    private fun toResponse(order: Order, method: String? = null): OrderResponse =
        OrderResponse.of(order, maskPhone(phoneCipher.decrypt(order.phoneEnc)), method)

    // 010-1234-5678 을 010-****-5678 로
    private fun maskPhone(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        if (digits.length < 8) return "***"
        return digits.take(3) + "-****-" + digits.takeLast(4)
    }

    // 드물지만 겹칠 수 있다. 한 번 더 만들어 보고 그래도 겹치면 예외
    private fun nextOrderNo(now: Instant): String {
        repeat(2) {
            val candidate = orderNoGenerator.generate(now)
            if (!orderRepository.existsByOrderNo(candidate)) return candidate
        }
        throw IllegalStateException("주문번호를 만들지 못했습니다")
    }

    private fun saveAddress(userId: Long, shipping: ShippingInfo) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId)?.unsetDefault()
        addressRepository.save(Address(userId, shipping, isDefault = true))
    }

    private companion object {
        const val EXPIRE_BATCH = 100
    }
}
