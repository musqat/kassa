package com.kassa.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class EmailRequest(
    @field:NotBlank
    @field:Email
    val email: String,
)

data class EmailTokenRequest(
    @field:NotBlank
    val token: String,
)
