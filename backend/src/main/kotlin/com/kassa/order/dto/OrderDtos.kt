package com.kassa.order.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

const val PHONE_REGEX = "^01[016-9]-?\\d{3,4}-?\\d{4}$"
const val ZIPCODE_REGEX = "^\\d{5}$"

data class PlaceOrderRequest(
    @field:NotBlank
    @field:Size(max = 50)
    val receiver: String,

    @field:Pattern(regexp = PHONE_REGEX, message = "휴대폰 번호 형식이 아닙니다")
    val phone: String,

    @field:Pattern(regexp = ZIPCODE_REGEX, message = "우편번호는 5자리 숫자입니다")
    val zipcode: String,

    @field:NotBlank
    @field:Size(max = 200)
    val addr1: String,

    @field:Size(max = 200)
    val addr2: String? = null,

    val saveAddress: Boolean = false,
) {
    // 전화번호는 로그에 남기지 않는다
    override fun toString() =
        "PlaceOrderRequest(receiver=$receiver, phone=***, zipcode=$zipcode, addr1=$addr1, addr2=$addr2)"
}

data class PlaceOrderResponse(
    val orderNo: String,
    val totalAmount: Long,
)
