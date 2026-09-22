package com.kassa.user.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank
    val email: String,

    @field:NotBlank
    val password: String,
) {
    override fun toString() = "LoginRequest(email=$email, password=***)"
}

data class TokenResponse(
    val accessToken: String,
    val expiresIn: Long,
)
