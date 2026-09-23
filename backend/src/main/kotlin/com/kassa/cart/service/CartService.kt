package com.kassa.cart.service

import com.kassa.cart.domain.CartItem
import com.kassa.cart.dto.AddItemRequest
import com.kassa.cart.dto.CartItemResponse
import com.kassa.cart.dto.CartResponse
import com.kassa.cart.repository.CartItemRepository
import com.kassa.catalog.domain.ProductStatus
import com.kassa.catalog.repository.ProductRepository
import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import com.kassa.pricing.ShippingPolicy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CartService(
    private val cartItemRepository: CartItemRepository,
    private val productRepository: ProductRepository,
) {

    /** 숨김 상품은 목록에서 빼고, 담을 수 없는 줄은 합계에서 뺀다 */
    @Transactional(readOnly = true)
    fun getCart(userId: Long): CartResponse {
        val items = cartItemRepository.findAllByUserIdOrderByIdDesc(userId)

        val visible = items.filter { it.product.status != ProductStatus.HIDDEN }

        val rows = visible.map { CartItemResponse.from(it) }

        val itemAmount = rows.filter { it.orderable }.sumOf { it.lineAmount }

        val shippingFee = ShippingPolicy.feeFor(itemAmount)

        return CartResponse(
            items = rows,
            itemAmount = itemAmount,
            shippingFee = shippingFee,
            totalAmount = itemAmount + shippingFee,
            freeShippingRemaining = ShippingPolicy.freeShippingRemaining(itemAmount),
        )
    }

    /** 이미 담긴 상품이면 수량을 더한다 */
    @Transactional
    fun addItem(userId: Long, request: AddItemRequest) {
        val product =
            productRepository.findByIdOrNull(request.productId) ?: throw BusinessException(ErrorCode.PRODUCT_NOT_FOUND)

        if (product.status == ProductStatus.HIDDEN) {
            throw BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
        }

        if (product.status != ProductStatus.ON_SALE) {
            throw BusinessException(ErrorCode.PRODUCT_NOT_ORDERABLE)
        }

        val existing = cartItemRepository.findByUserIdAndProductId(userId, request.productId)

        if (existing != null) {
            existing.increase(request.quantity)
        } else {
            cartItemRepository.save(CartItem(userId, product, request.quantity))
        }
    }

    @Transactional
    fun changeQuantity(userId: Long, itemId: Long, quantity: Int) {
        val item =
            cartItemRepository.findByIdAndUserId(itemId, userId)
                ?: throw BusinessException(ErrorCode.CART_ITEM_NOT_FOUND)

        item.changeQuantity(quantity)
    }

    @Transactional
    fun remove(userId: Long, itemId: Long) {
        val item =
            cartItemRepository.findByIdAndUserId(itemId, userId)
                ?: throw BusinessException(ErrorCode.CART_ITEM_NOT_FOUND)

        cartItemRepository.delete(item)
    }
}
