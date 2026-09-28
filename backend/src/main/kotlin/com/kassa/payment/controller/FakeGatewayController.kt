package com.kassa.payment.controller

import com.kassa.payment.gateway.FakePaymentGateway
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// 브라우저에서 실패 상황을 고르는 창구. 스위치가 서버 쪽 빈에 있어 프런트가 직접 못 바꾼다
// 배포 프로파일에서는 FakePaymentGateway 와 함께 빈이 올라오지 않는다
@Tag(name = "가짜 결제 스위치")
@RestController
@RequestMapping("/api/admin/fake-gateway")
@Profile("local", "test")
class FakeGatewayController(private val gateway: FakePaymentGateway) {

    @Operation(summary = "다음 승인 결과 고르기", description = "SUCCESS·FAIL·TIMEOUT 중 하나")
    @PostMapping("/outcome")
    fun setOutcome(@RequestParam outcome: FakePaymentGateway.Outcome) {
        gateway.nextResult = outcome
    }

    @Operation(summary = "금액 불일치 만들기", description = "승인 요청 금액을 이 값으로 바꾼다")
    @PostMapping("/amount")
    fun setAmount(@RequestParam amount: Long?) {
        gateway.amountOverride = amount
    }

    @Operation(summary = "취소 실패 횟수", description = "보상이 NEEDS_ATTENTION 까지 가는지 볼 때")
    @PostMapping("/cancel-fail")
    fun setCancelFail(@RequestParam count: Int) {
        gateway.cancelFailCount = count
    }

    @Operation(summary = "스위치 초기화")
    @PostMapping("/reset")
    fun reset() {
        gateway.reset()
    }
}
