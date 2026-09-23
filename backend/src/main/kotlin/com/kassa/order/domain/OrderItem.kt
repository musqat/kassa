package com.kassa.order.domain

import com.kassa.catalog.domain.Product
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

// 상품명·가격은 주문 시점 값을 복사한다. 상품이 바뀌어도 주문서는 그대로다
@Entity
@Table(name = "order_item")
class OrderItem(
    productId: Long,
    productName: String,
    price: Long,
    quantity: Int,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    var order: Order? = null
        protected set

    var productId: Long = productId
        protected set

    var productName: String = productName
        protected set

    var price: Long = price
        protected set

    var quantity: Int = quantity
        protected set

    val lineAmount: Long
        get() = price * quantity

    fun belongTo(order: Order) {
        this.order = order
    }

    companion object {
        fun of(product: Product, quantity: Int) =
            OrderItem(product.id!!, product.name, product.price, quantity)
    }
}
