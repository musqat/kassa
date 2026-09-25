package com.kassa.order.repository

import com.kassa.order.domain.Order
import com.kassa.order.domain.OrderStatus
import java.time.Instant
import jakarta.persistence.LockModeType
import jakarta.persistence.QueryHint
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.data.repository.query.Param
import org.springframework.data.jpa.repository.JpaRepository

interface OrderRepository : JpaRepository<Order, Long> {

    // 남의 주문을 건드리지 못하게 회원 조건을 같이 건다
    @EntityGraph(attributePaths = ["items"])
    fun findByOrderNoAndUserId(orderNo: String, userId: Long): Order?

    @EntityGraph(attributePaths = ["items"])
    fun findAllByUserIdOrderByIdDesc(userId: Long): List<Order>

    fun existsByOrderNo(orderNo: String): Boolean

    // 다른 워커가 잡은 행은 건너뛴다. 인스턴스를 늘려도 같은 주문을 두 번 처리하지 않는다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("select o from Order o where o.status = :status and o.createdAt < :cutoff order by o.id")
    fun findExpired(
        @Param("status") status: OrderStatus,
        @Param("cutoff") cutoff: Instant,
        limit: Limit,
    ): List<Order>
}
