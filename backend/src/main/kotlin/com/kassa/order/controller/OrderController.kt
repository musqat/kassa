package com.kassa.order.controller

import com.kassa.common.security.userId
import com.kassa.order.dto.OrderResponse
import com.kassa.order.dto.PlaceOrderRequest
import com.kassa.order.dto.PlaceOrderResponse
import com.kassa.order.service.OrderService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "주문")
@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService,
) {

    @Operation(
        summary = "주문 생성",
        description = "장바구니에서 판매 중인 상품만 담아 주문을 만들고 재고를 선점한다. 장바구니는 그대로 둔다",
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun place(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: PlaceOrderRequest,
    ): PlaceOrderResponse = orderService.place(jwt.userId(), request)

    @Operation(summary = "주문 목록", description = "내 주문만 최신순으로")
    @GetMapping
    fun findMyOrders(@AuthenticationPrincipal jwt: Jwt): List<OrderResponse> =
        orderService.findMyOrders(jwt.userId())

    @Operation(summary = "주문 단건", description = "남의 주문번호는 404")
    @GetMapping("/{orderNo}")
    fun findMyOrder(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable orderNo: String,
    ): OrderResponse = orderService.findMyOrder(jwt.userId(), orderNo)

    @Operation(summary = "주문 취소", description = "결제 전 주문만 취소할 수 있다. 선점한 재고가 풀린다")
    @PostMapping("/{orderNo}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun cancel(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable orderNo: String,
    ) {
        orderService.cancel(jwt.userId(), orderNo)
    }
}
