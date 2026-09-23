package com.kassa.cart.repository

import com.kassa.cart.domain.CartItem
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface CartItemRepository : JpaRepository<CartItem, Long> {

    // 항목마다 상품을 따로 읽지 않도록 한 번에 가져온다
    @EntityGraph(attributePaths = ["product"])
    fun findAllByUserIdOrderByIdDesc(userId: Long): List<CartItem>

    // 남의 항목을 건드리지 못하게 회원 조건을 같이 건다
    fun findByIdAndUserId(id: Long, userId: Long): CartItem?

    fun findByUserIdAndProductId(userId: Long, productId: Long): CartItem?

    fun deleteAllByUserId(userId: Long)
}
