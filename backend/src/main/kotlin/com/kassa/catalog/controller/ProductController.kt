package com.kassa.catalog.controller

import com.kassa.catalog.service.ProductService
import com.kassa.catalog.dto.ProductResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "상품")
@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService,
) {

    /** GET /api/products?categoryId=1 */
    @Operation(summary = "상품 목록", description = "categoryId 를 주면 그 분류만, 숨김 상품은 빼고 준다")
    @GetMapping
    fun findAll(
        @RequestParam(required = false) categoryId: Long?,
    ): List<ProductResponse> = productService.findAll(categoryId)

    /** GET /api/products/1 */
    @Operation(summary = "상품 단건")
    @GetMapping("/{id}")
    fun findOne(
        @PathVariable id: Long,
    ): ProductResponse = productService.findOne(id)
}
