package com.kassa.user.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank
    val loginId: String,

    @field:NotBlank
    val password: String,
) {
    override fun toString() = "LoginRequest(loginId=$loginId, password=***)"
}

data class TokenResponse(
    val accessToken: String,
    val expiresIn: Long,
)
