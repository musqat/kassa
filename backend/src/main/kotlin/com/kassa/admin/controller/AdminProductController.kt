package com.kassa.admin.controller

import com.kassa.admin.dto.AdminProductResponse
import com.kassa.admin.dto.CategoryResponse
import com.kassa.admin.dto.ChangeStatusRequest
import com.kassa.admin.dto.ChangeStockRequest
import com.kassa.admin.dto.CreateProductRequest
import com.kassa.admin.dto.EditProductRequest
import com.kassa.admin.service.AdminProductService
import com.kassa.catalog.domain.ProductStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

// 권한은 SecurityConfig 에서 /api/admin/** 통째로 막는다
@Tag(name = "관리자 상품")
@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/api/admin/products")
class AdminProductController(
    private val adminProductService: AdminProductService,
) {

    @Operation(summary = "상품 목록", description = "숨김과 판매 종료까지 본다. status 로 거를 수 있다")
    @GetMapping
    fun findAll(@RequestParam(required = false) status: ProductStatus?): List<AdminProductResponse> =
        adminProductService.findAll(status)

    @Operation(summary = "상품 등록")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateProductRequest): AdminProductResponse =
        adminProductService.create(request)

    @Operation(summary = "상품 수정", description = "이름·가격·분류·사진. 재고와 상태는 따로 고친다")
    @PatchMapping("/{id}")
    fun edit(
        @PathVariable id: Long,
        @Valid @RequestBody request: EditProductRequest,
    ): AdminProductResponse = adminProductService.edit(id, request)

    @Operation(summary = "재고 수정", description = "절대값. 선점된 수량보다 적게는 못 내린다")
    @PatchMapping("/{id}/stock")
    fun changeStock(
        @PathVariable id: Long,
        @Valid @RequestBody request: ChangeStockRequest,
    ): AdminProductResponse = adminProductService.changeStock(id, request)

    @Operation(summary = "상태 변경", description = "판매 종료(DELETED)는 되돌리지 않는다")
    @PatchMapping("/{id}/status")
    fun changeStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: ChangeStatusRequest,
    ): AdminProductResponse = adminProductService.changeStatus(id, request)

    @Operation(summary = "상품 삭제", description = "주문에 들어간 적 있으면 409. 판매 종료로 바꾼다")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long) {
        adminProductService.delete(id)
    }
}

@Tag(name = "관리자 상품")
@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/api/admin/categories")
class AdminCategoryController(
    private val adminProductService: AdminProductService,
) {

    @Operation(summary = "분류 목록", description = "상품 등록·수정에서 고른다")
    @GetMapping
    fun findAll(): List<CategoryResponse> = adminProductService.findCategories()
}
