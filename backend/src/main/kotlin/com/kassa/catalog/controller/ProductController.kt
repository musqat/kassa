package com.kassa.catalog.controller

import com.kassa.catalog.service.ProductService
import com.kassa.catalog.dto.ProductResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService,
) {

    /** GET /api/products?categoryId=1 */
    @GetMapping
    fun findAll(
        @RequestParam(required = false) categoryId: Long?,
    ): List<ProductResponse> = productService.findAll(categoryId)

    /** GET /api/products/1 */
    @GetMapping("/{id}")
    fun findOne(
        @PathVariable id: Long,
    ): ProductResponse = productService.findOne(id)
}
