package com.kassa.admin.controller

import com.kassa.admin.dto.AdminAttentionResponse
import com.kassa.admin.service.AdminAttentionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "관리자 주문")
@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/api/admin/attention")
class AdminAttentionController(
    private val adminAttentionService: AdminAttentionService,
) {

    @Operation(summary = "확인 필요 주문", description = "10분 넘게 결과를 모르는 결제와 멈춘 사가. 오래된 순")
    @GetMapping
    fun findAll(): List<AdminAttentionResponse> = adminAttentionService.findAll()
}
