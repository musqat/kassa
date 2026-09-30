package com.kassa.admin.service

import com.kassa.admin.dto.AdminProductResponse
import com.kassa.admin.dto.CategoryResponse
import com.kassa.admin.dto.ChangeStatusRequest
import com.kassa.admin.dto.ChangeStockRequest
import com.kassa.admin.dto.CreateProductRequest
import com.kassa.admin.dto.EditProductRequest
import com.kassa.cart.repository.CartItemRepository
import com.kassa.catalog.domain.Product
import com.kassa.catalog.domain.ProductStatus
import com.kassa.catalog.repository.CategoryRepository
import com.kassa.catalog.repository.ProductRepository
import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import com.kassa.order.repository.OrderItemRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 관리자 상품 관리. 회원용 조회(ProductService)와 보는 범위가 다르다
@Service
class AdminProductService(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val cartItemRepository: CartItemRepository,
    private val orderItemRepository: OrderItemRepository,
) {

    @Transactional(readOnly = true)
    fun findAll(status: ProductStatus?): List<AdminProductResponse> {
        val statuses = if (status == null) ProductStatus.entries.toList() else listOf(status)

        return productRepository.findByStatusIn(statuses).map { AdminProductResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun findCategories(): List<CategoryResponse> =
        categoryRepository.findAll().map { CategoryResponse.from(it) }

    @Transactional
    fun create(request: CreateProductRequest): AdminProductResponse {
        val category = categoryRepository.findById(request.categoryId).orElseThrow {
            BusinessException(ErrorCode.INVALID_REQUEST, "없는 분류입니다")
        }

        val product = Product(
            category = category,
            name = request.name,
            price = request.price,
            stock = request.stock,
            status = request.status,
            thumbnailUrl = request.thumbnailUrl,
        )
        return AdminProductResponse.from(productRepository.save(product))
    }

    @Transactional
    fun edit(id: Long, request: EditProductRequest): AdminProductResponse {
        val product = find(id)
        val category = categoryRepository.findById(request.categoryId).orElseThrow {
            BusinessException(ErrorCode.INVALID_REQUEST, "없는 분류입니다")
        }

        product.edit(request.name, request.price, request.thumbnailUrl, category)

        return AdminProductResponse.from(product)
    }

    @Transactional
    fun changeStock(id: Long, request: ChangeStockRequest): AdminProductResponse {
        val product = find(id)
        product.changeStock(request.stock)

        return AdminProductResponse.from(product)
    }

    @Transactional
    fun changeStatus(id: Long, request: ChangeStatusRequest): AdminProductResponse {
        val product = find(id)
        product.changeStatus(request.status)

        return AdminProductResponse.from(product)
    }

    @Transactional
    fun delete(id: Long) {
        val product = find(id)

        // 주문에 들어간 적 있으면 지우지 않는다. 주문 이력이 이 상품을 가리킨다
        if (orderItemRepository.existsByProductId(id)) {
            throw BusinessException(ErrorCode.PRODUCT_NOT_DELETABLE)
        }
        // 상품이 없으면 주문도 못 한다. 담아 둔 항목은 같이 지운다
        cartItemRepository.deleteAllByProductId(id)
        productRepository.delete(product)
    }

    private fun find(id: Long): Product =
        productRepository.findById(id).orElseThrow { BusinessException(ErrorCode.PRODUCT_NOT_FOUND) }
}
