package com.kassa.catalog.domain

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
}
