package com.kassa.payment.controller

import com.kassa.payment.inbox.WebhookReceiver
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "결제")
@RestController
@RequestMapping("/api/payments")
class WebhookController(private val receiver: WebhookReceiver) {

    @Operation(
        summary = "결제 웹훅 수신",
        description = "검증하고 인박스에 저장만 한다. 서명이 틀려도 200 을 준다",
    )
    @PostMapping("/webhook")
    fun receive(
        @RequestBody rawBody: String,
        request: HttpServletRequest,
    ) {
        // 서명은 원문 그대로 계산한다. 객체로 받으면 직렬화가 달라져 검증이 깨진다
        val headers = request.headerNames.toList().associateWith { request.getHeader(it) }
        receiver.receive(headers, rawBody)
    }
}
