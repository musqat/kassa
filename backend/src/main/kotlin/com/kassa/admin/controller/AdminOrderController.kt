package com.kassa.admin.controller

import com.kassa.admin.dto.AdminOrderDetailResponse
import com.kassa.admin.dto.AdminOrderResponse
import com.kassa.admin.service.AdminOrderService
import com.kassa.order.domain.OrderStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// 권한은 SecurityConfig 에서 /api/admin/** 통째로 막는다
@Tag(name = "관리자 주문")
@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/api/admin/orders")
class AdminOrderController(
    private val adminOrderService: AdminOrderService,
) {

    @Operation(summary = "주문 목록", description = "회원을 가리지 않고 최신순. status 로 거를 수 있다")
    @GetMapping
    fun findAll(@RequestParam(required = false) status: OrderStatus?): List<AdminOrderResponse> =
        adminOrderService.findAll(status)

    @Operation(summary = "주문 단건", description = "주문 줄과 배송지, 결제 수단까지")
    @GetMapping("/{orderNo}")
    fun find(@PathVariable orderNo: String): AdminOrderDetailResponse =
        adminOrderService.find(orderNo)

    @Operation(summary = "배송 처리", description = "결제된 주문만. 다른 상태는 409")
    @PatchMapping("/{orderNo}/ship")
    fun markShipped(@PathVariable orderNo: String): AdminOrderResponse =
        adminOrderService.markShipped(orderNo)
}
