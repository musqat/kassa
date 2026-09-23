package com.kassa.catalog.domain

import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

enum class ProductStatus {
    ON_SALE,
    SOLD_OUT,
    HIDDEN,
    ;

    companion object {
        /** 목록과 단건 조회에 노출되는 상태. HIDDEN 은 빠진다. */
        val VISIBLE = listOf(ON_SALE, SOLD_OUT)
    }
}

@Entity
class Product(
    category: Category,
    name: String,
    price: Long,
    stock: Int = 0,
    status: ProductStatus = ProductStatus.ON_SALE,
    thumbnailUrl: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: Category = category
        protected set
    var name: String = name
        protected set
    var price: Long = price
        protected set

    @Enumerated(EnumType.STRING)
    var status: ProductStatus = status
        protected set
    var thumbnailUrl: String? = thumbnailUrl
        protected set
    var stock: Int = stock
        protected set
    var reservedStock: Int = 0
        protected set

    /** 팔 수 있는 수량. 선점된 만큼은 이미 남의 것이다 */
    fun availableStock(): Int {
        return stock - reservedStock
    }

    /** 주문이 만들어질 때 수량을 잡아 둔다 */
    fun reserve(quantity: Int) {
        if (quantity > availableStock()) {
            throw BusinessException(ErrorCode.OUT_OF_STOCK)
        }
        reservedStock += quantity
    }

    /** 취소·만료로 잡아 둔 수량을 푼다 */
    fun releaseReservation(quantity: Int) {
        check(quantity <= reservedStock) { "선점한 수량보다 많이 풀 수 없습니다" }
        reservedStock -= quantity
    }

    /** 결제가 끝나 실제로 나간다. stock 과 reservedStock 을 함께 줄인다 */
    fun confirmReservation(quantity: Int) {
        check(quantity <= reservedStock) { "선점한 수량보다 많이 풀 수 없습니다" }
        reservedStock -= quantity
        stock -= quantity
    }

    fun changePrice(price: Long) {
        this.price = price
    }

    fun changeStatus(status: ProductStatus) {
        this.status = status
    }
}
