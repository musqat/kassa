package com.kassa.catalog.dto

import com.kassa.catalog.domain.Product
import com.kassa.catalog.domain.ProductStatus

/**
 * 상품 응답.
 *
 * stock 과 reservedStock 은 넣지 않는다. 재고 수량은 매출 추정 근거가 되고
 * 판매 가능 여부는 status 로 충분하다.
 */
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
