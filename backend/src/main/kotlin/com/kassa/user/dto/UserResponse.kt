package com.kassa.user.dto

import com.kassa.user.domain.User

data class UserResponse(
    val id: Long,
    val email: String,
    val name: String,
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id!!,
            email = user.email,
            name = user.name,
        )
    }
}
