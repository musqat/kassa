package com.kassa.cart.dto

import com.kassa.cart.domain.CartItem
import com.kassa.catalog.domain.ProductStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class AddItemRequest(
    @field:NotNull
    val productId: Long,

    @field:Min(1)
    @field:Max(99)
    val quantity: Int,
)

data class UpdateQuantityRequest(
    @field:Min(1)
    @field:Max(99)
    val quantity: Int,
)

data class CartItemResponse(
    val itemId: Long,
    val productId: Long,
    val name: String,
    val price: Long,
    val quantity: Int,
    val lineAmount: Long,
    val orderable: Boolean,
) {
    companion object {
        fun from(item: CartItem): CartItemResponse {
            val product = item.product
            return CartItemResponse(
                itemId = item.id!!,
                productId = product.id!!,
                name = product.name,
                price = product.price,
                lineAmount = product.price * item.quantity,
                quantity = item.quantity,
                orderable = product.status == ProductStatus.ON_SALE,
            )
        }
    }
}

data class CartResponse(
    val items: List<CartItemResponse>,
    val itemAmount: Long,
    val shippingFee: Long,
    val totalAmount: Long,
    val freeShippingRemaining: Long,
)
