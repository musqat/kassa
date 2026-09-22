package com.kassa.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class SignupRequest(
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
    override fun toString() = "SignupRequest(email=$email, password=***, name=$name)"
}
