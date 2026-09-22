package com.kassa.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

const val LOGIN_ID_REGEX = "^[a-z0-9_]{4,20}$"
const val LOGIN_ID_MESSAGE = "4~20자, 영문 소문자·숫자·_ 만 쓸 수 있습니다"

data class SignupRequest(
    @field:Pattern(regexp = LOGIN_ID_REGEX, message = LOGIN_ID_MESSAGE)
    val loginId: String,

    @field:NotBlank
    @field:Email
    val email: String,

    @field:Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$",
        message = "8~64자, 영문과 숫자를 모두 포함해야 합니다",
    )
    val password: String,

    @field:NotBlank
    @field:Size(max = 50)
    val name: String,
) {
    // 비밀번호는 가린다
    override fun toString() = "SignupRequest(loginId=$loginId, email=$email, password=***, name=$name)"
}
