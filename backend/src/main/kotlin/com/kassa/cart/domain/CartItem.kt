package com.kassa.cart.domain

import com.kassa.catalog.domain.Product
import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

// 가격은 담지 않는다. 보여줄 때마다 상품에서 읽는다
@Entity
@Table(name = "cart_item")
class CartItem(
    userId: Long,
    product: Product,
    quantity: Int,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    var userId: Long = userId
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    var product: Product = product
        protected set

    var quantity: Int = quantity
        protected set

    @Column(insertable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    @Column(insertable = false, updatable = false)
    var updatedAt: Instant = Instant.now()
        protected set

    /** 이미 담긴 상품을 또 담으면 수량을 더한다 */
    fun increase(amount: Int) {
        val next = quantity + amount
        if (next > MAX_QUANTITY) {
            throw BusinessException(ErrorCode.CART_QUANTITY_EXCEEDED)
        }
        quantity = next
    }

    fun changeQuantity(quantity: Int) {
        if (quantity !in 1..MAX_QUANTITY){
            throw BusinessException(ErrorCode.CART_QUANTITY_EXCEEDED)
        }
        this.quantity = quantity
    }

    companion object {
        const val MAX_QUANTITY = 99
    }
}
