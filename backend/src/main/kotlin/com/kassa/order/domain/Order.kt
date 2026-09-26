package com.kassa.order.domain

import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant

// order 는 SQL 예약어라 테이블 이름이 orders 다
@Entity
@Table(name = "orders")
class Order(
    orderNo: String,
    userId: Long,
    itemAmount: Long,
    shippingFee: Long,
    shipping: ShippingInfo,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    var orderNo: String = orderNo
        protected set

    var userId: Long = userId
        protected set

    var itemAmount: Long = itemAmount
        protected set

    var shippingFee: Long = shippingFee
        protected set

    var totalAmount: Long = itemAmount + shippingFee
        protected set

    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.PENDING
        protected set

    var receiver: String = shipping.receiver
        protected set

    var phoneEnc: String = shipping.phoneEnc
        protected set

    var zipcode: String = shipping.zipcode
        protected set

    var addr1: String = shipping.addr1
        protected set

    var addr2: String? = shipping.addr2
        protected set

    @Column(insertable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    var paidAt: Instant? = null
        protected set

    var closedAt: Instant? = null
        protected set

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    var items: MutableList<OrderItem> = mutableListOf()
        protected set

    /** 결제 완료 저장. 이미 PAID 면 그대로 둔다 */
    fun markPaid(now: Instant) {
        when (status) {
            OrderStatus.PENDING -> {
                status = OrderStatus.PAID
                paidAt = now
            }
            // 승인과 웹훅이 순서 없이 들어와 두 번 부르는 게 정상이다
            OrderStatus.PAID -> Unit
            else -> throw BusinessException(ErrorCode.ORDER_NOT_PAYABLE)
        }
    }

    /** 취소 저장. PENDING 에서만 */
    fun cancel(now: Instant) {
        if (status != OrderStatus.PENDING) {
            throw BusinessException(ErrorCode.ORDER_NOT_CANCELABLE)
        }

        status = OrderStatus.CANCELED
        closedAt = now
    }

    /** 만료 저장. PENDING 에서만 */
    fun expire(now: Instant) {
        if (status != OrderStatus.PENDING) {
            throw BusinessException(ErrorCode.ORDER_NOT_CANCELABLE)
        }
        status = OrderStatus.FAILED
        closedAt = now
    }

    fun addItem(item: OrderItem) {
        items += item
        item.belongTo(this)
    }
}

// 주문 시점의 배송지를 값으로 옮긴다
data class ShippingInfo(
    val receiver: String,
    val phoneEnc: String,
    val zipcode: String,
    val addr1: String,
    val addr2: String?,
)
