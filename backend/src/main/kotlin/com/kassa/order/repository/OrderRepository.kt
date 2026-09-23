package com.kassa.order.repository

import com.kassa.order.domain.Order
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long> {

    // 남의 주문을 건드리지 못하게 회원 조건을 같이 건다
    @EntityGraph(attributePaths = ["items"])
    fun findByOrderNoAndUserId(orderNo: String, userId: Long): Order?

    @EntityGraph(attributePaths = ["items"])
    fun findAllByUserIdOrderByIdDesc(userId: Long): List<Order>

    fun existsByOrderNo(orderNo: String): Boolean
}
