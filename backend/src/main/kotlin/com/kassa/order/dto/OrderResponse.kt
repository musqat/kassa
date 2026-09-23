package com.kassa.order.dto

import com.kassa.order.domain.Order
import com.kassa.order.domain.OrderStatus
import java.time.Instant

data class OrderItemResponse(
    val productId: Long,
    val name: String,
    val price: Long,
    val quantity: Int,
    val lineAmount: Long,
)

data class OrderResponse(
    val orderNo: String,
    val status: OrderStatus,
    val itemAmount: Long,
    val shippingFee: Long,
    val totalAmount: Long,
    val items: List<OrderItemResponse>,
    val receiver: String,
    val phone: String,
    val zipcode: String,
    val addr1: String,
    val addr2: String?,
    val createdAt: Instant,
) {
    companion object {
        // phone 은 복호화한 뒤 가린 값을 받는다
        fun of(order: Order, phone: String) = OrderResponse(
            orderNo = order.orderNo,
            status = order.status,
            itemAmount = order.itemAmount,
            shippingFee = order.shippingFee,
            totalAmount = order.totalAmount,
            items = order.items.map {
                OrderItemResponse(it.productId, it.productName, it.price, it.quantity, it.lineAmount)
            },
            receiver = order.receiver,
            phone = phone,
            zipcode = order.zipcode,
            addr1 = order.addr1,
            addr2 = order.addr2,
            createdAt = order.createdAt,
        )
    }
}
