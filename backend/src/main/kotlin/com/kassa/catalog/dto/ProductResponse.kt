package com.kassa.catalog.dto

import com.kassa.catalog.domain.Product
import com.kassa.catalog.domain.ProductStatus

// 재고 수량은 내보내지 않는다
data class ProductResponse(
    val id: Long,
    val categoryId: Long,
    val name: String,
    val price: Long,
    val status: ProductStatus,
    val thumbnailUrl: String?,
) {
    companion object {
        fun from(product: Product): ProductResponse {
            return ProductResponse(
                id = product.id!!,
                categoryId = product.category.id!!,
                name = product.name,
                price = product.price,
                status = product.status,
                thumbnailUrl = product.thumbnailUrl,
            )
        }
    }
}
