package com.kassa.cart.controller

import com.kassa.cart.dto.AddItemRequest
import com.kassa.cart.dto.CartResponse
import com.kassa.cart.dto.UpdateQuantityRequest
import com.kassa.cart.service.CartService
import com.kassa.common.security.userId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "장바구니")
@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/api/cart")
class CartController(
    private val cartService: CartService,
) {

    @Operation(summary = "장바구니 조회", description = "품절 줄은 합계에서 빠지고, 숨김 상품은 목록에서 빠진다")
    @GetMapping
    fun getCart(@AuthenticationPrincipal jwt: Jwt): CartResponse =
        cartService.getCart(jwt.userId())

    @Operation(summary = "담기", description = "이미 담긴 상품이면 수량을 더한다")
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    fun addItem(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: AddItemRequest) {
        cartService.addItem(jwt.userId(), request)
    }

    @Operation(summary = "수량 변경")
    @PatchMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changeQuantity(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable itemId: Long,
        @Valid @RequestBody request: UpdateQuantityRequest,
    ) {
        cartService.changeQuantity(jwt.userId(), itemId, request.quantity)
    }

    @Operation(summary = "항목 삭제")
    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(@AuthenticationPrincipal jwt: Jwt, @PathVariable itemId: Long) {
        cartService.remove(jwt.userId(), itemId)
    }
}
