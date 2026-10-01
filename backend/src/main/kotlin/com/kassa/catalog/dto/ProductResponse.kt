package com.kassa.catalog.dto

import com.kassa.catalog.domain.Product
import com.kassa.catalog.domain.ProductStatus

// 재고 수량은 내보내지 않는다. 살 수 있는지만 soldOut 으로 알린다
data class ProductResponse(
    val id: Long,
    val categoryId: Long,
    val name: String,
    val price: Long,
    val status: ProductStatus,
    val thumbnailUrl: String?,
    // 상태가 판매 중이어도 남은 수량이 없으면 참이다. 상태는 관리자가 손으로 바꾼다
    val soldOut: Boolean,
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
                soldOut = product.status != ProductStatus.ON_SALE || product.availableStock() <= 0,
            )
        }
    }
}
