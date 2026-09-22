package com.kassa.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class EmailRequest(
    @field:NotBlank
    @field:Email
    val email: String,
)

data class EmailTokenRequest(
    @field:NotBlank
    val token: String,
)

data class PasswordResetRequest(
    @field:NotBlank
    val token: String,

    @field:Pattern(regexp = PASSWORD_REGEX, message = PASSWORD_MESSAGE)
    val newPassword: String,
) {
    override fun toString() = "PasswordResetRequest(token=***, newPassword=***)"
}

data class WithdrawRequest(
    @field:NotBlank
    val password: String,
) {
    override fun toString() = "WithdrawRequest(password=***)"
}
