package com.kassa.user.repository

import com.kassa.user.domain.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {

    fun findByLoginId(loginId: String): User?

    fun existsByLoginId(loginId: String): Boolean

    fun findByEmail(email: String): User?

    fun existsByEmail(email: String): Boolean
}
