package com.kassa.order.repository

import com.kassa.order.domain.OrderItem
import org.springframework.data.jpa.repository.JpaRepository

interface OrderItemRepository : JpaRepository<OrderItem, Long> {

    // 주문에 들어간 적 있는 상품은 지울 수 없다
    fun existsByProductId(productId: Long): Boolean
}
