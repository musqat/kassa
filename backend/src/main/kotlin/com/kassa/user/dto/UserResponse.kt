package com.kassa.user.dto

import com.kassa.user.domain.User

data class UserResponse(
    val id: Long,
    val loginId: String,
    val email: String,
    val name: String,
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id!!,
            loginId = user.loginId,
            email = user.email,
            name = user.name,
        )
    }
}

data class LoginIdAvailabilityResponse(
    val available: Boolean,
)
