package com.kassa.catalog.repository

import com.kassa.catalog.domain.Product
import com.kassa.catalog.domain.ProductStatus
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query

interface ProductRepository : JpaRepository<Product, Long> {

    /** 노출 대상 전체. HIDDEN 은 빠진다. */
    fun findByStatusIn(statuses: List<ProductStatus>): List<Product>

    /** 카테고리별 노출 대상. */
    fun findByCategoryIdAndStatusIn(categoryId: Long, statuses: List<ProductStatus>): List<Product>

    // 재고를 건드리기 전에 행을 잠근다. 교착을 피하려고 id 오름차순으로 잡는다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id in :ids order by p.id")
    fun findAllForUpdate(ids: List<Long>): List<Product>
}
