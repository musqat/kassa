package com.kassa.catalog.repository

import com.kassa.catalog.domain.Product
import com.kassa.catalog.domain.ProductStatus
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository : JpaRepository<Product, Long> {

    /** 노출 대상 전체. HIDDEN 은 빠진다. */
    fun findByStatusIn(statuses: List<ProductStatus>): List<Product>

    /** 카테고리별 노출 대상. */
    fun findByCategoryIdAndStatusIn(categoryId: Long, statuses: List<ProductStatus>): List<Product>
}
