package com.kassa.catalog.service

import com.kassa.catalog.domain.ProductStatus
import com.kassa.catalog.dto.ProductResponse
import com.kassa.catalog.repository.ProductRepository
import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProductService(
    private val productRepository: ProductRepository,
) {

    /** 노출 대상 목록. categoryId 가 있으면 그 카테고리만. */
    fun findAll(categoryId: Long?): List<ProductResponse> {
        val products = if (categoryId == null) {
            productRepository.findByStatusIn(ProductStatus.VISIBLE)
        }else{
            productRepository.findByCategoryIdAndStatusIn(categoryId, ProductStatus.VISIBLE)
        }

        return products.map(ProductResponse::from)
    }

    /** 단건 조회 */
    fun findOne(id: Long): ProductResponse {
        val product = productRepository.findByIdOrNull(id) ?: throw BusinessException(ErrorCode.PRODUCT_NOT_FOUND)

        if (product.status !in ProductStatus.VISIBLE) {
            throw BusinessException(ErrorCode.PRODUCT_NOT_FOUND)
        }

        return ProductResponse.from(product)
    }
}
