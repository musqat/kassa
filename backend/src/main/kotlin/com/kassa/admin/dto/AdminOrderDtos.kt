package com.kassa.admin.dto

import com.kassa.order.domain.Order
import com.kassa.order.domain.OrderStatus
import com.kassa.order.dto.OrderItemResponse
import java.time.Instant

// 목록용. 누가 주문했는지 보이게 회원 아이디를 같이 준다
data class AdminOrderResponse(
    val orderNo: String,
    val status: OrderStatus,
    val loginId: String?,
    val receiver: String,
    val totalAmount: Long,
    val createdAt: Instant,
    val paidAt: Instant?,
    val shippedAt: Instant?,
    val closedAt: Instant?,
) {
    companion object {
        fun of(order: Order, loginId: String?) = AdminOrderResponse(
            orderNo = order.orderNo,
            status = order.status,
            loginId = loginId,
            receiver = order.receiver,
            totalAmount = order.totalAmount,
            createdAt = order.createdAt,
            paidAt = order.paidAt,
            shippedAt = order.shippedAt,
            closedAt = order.closedAt,
        )
    }
}

// 단건용. 주문 줄과 배송지까지 붙인다. 전화번호는 가린 값이 온다
data class AdminOrderDetailResponse(
    val order: AdminOrderResponse,
    val items: List<OrderItemResponse>,
    val phone: String,
    val zipcode: String,
    val addr1: String,
    val addr2: String?,
    val method: String?,
) {
    companion object {
        fun of(order: Order, loginId: String?, phone: String, method: String?) =
            AdminOrderDetailResponse(
                order = AdminOrderResponse.of(order, loginId),
                items = order.items.map {
                    OrderItemResponse(
                        productId = it.productId,
                        name = it.productName,
                        price = it.price,
                        quantity = it.quantity,
                        lineAmount = it.price * it.quantity,
                    )
                },
                phone = phone,
                zipcode = order.zipcode,
                addr1 = order.addr1,
                addr2 = order.addr2,
                method = method,
            )
    }
}
