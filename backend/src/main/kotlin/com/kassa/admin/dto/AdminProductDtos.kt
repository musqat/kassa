package com.kassa.admin.dto

import com.kassa.catalog.domain.Category
import com.kassa.catalog.domain.Product
import com.kassa.catalog.domain.ProductStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size

// 회원용 응답과 달리 재고와 선점 수량까지 보여준다
data class AdminProductResponse(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val name: String,
    val price: Long,
    val status: ProductStatus,
    val thumbnailUrl: String?,
    val stock: Int,
    val reservedStock: Int,
) {
    companion object {
        fun from(product: Product) = AdminProductResponse(
            id = product.id!!,
            categoryId = product.category.id!!,
            categoryName = product.category.name,
            name = product.name,
            price = product.price,
            status = product.status,
            thumbnailUrl = product.thumbnailUrl,
            stock = product.stock,
            reservedStock = product.reservedStock,
        )
    }
}

data class CreateProductRequest(
    val categoryId: Long,

    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:PositiveOrZero
    val price: Long,

    @field:PositiveOrZero
    val stock: Int,

    @field:Size(max = 500)
    val thumbnailUrl: String? = null,

    val status: ProductStatus = ProductStatus.ON_SALE,
)

// 재고와 상태는 검사가 달라 따로 받는다
data class EditProductRequest(
    val categoryId: Long,

    @field:NotBlank
    @field:Size(max = 100)
    val name: String,

    @field:PositiveOrZero
    val price: Long,

    @field:Size(max = 500)
    val thumbnailUrl: String? = null,
)

data class ChangeStockRequest(
    @field:PositiveOrZero
    val stock: Int,
)

data class ChangeStatusRequest(
    val status: ProductStatus,
)

data class CategoryResponse(
    val id: Long,
    val name: String,
) {
    companion object {
        fun from(category: Category) = CategoryResponse(category.id!!, category.name)
    }
}
