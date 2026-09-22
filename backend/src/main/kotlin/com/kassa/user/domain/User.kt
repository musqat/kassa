package com.kassa.user.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Duration
import java.time.Instant

// user 는 PostgreSQL 예약어
@Entity
@Table(name = "users")
class User(
    email: String,
    passwordHash: String,
    name: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    var email: String = email.trim().lowercase()
        protected set

    var passwordHash: String = passwordHash
        protected set

    var name: String = name
        protected set

    var loginFailCount: Int = 0
        protected set

    var lockedUntil: Instant? = null
        protected set

    @Column(insertable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    fun isLocked(now: Instant): Boolean {
        return lockedUntil?.isAfter(now) == true
    }

    /** 5회째 실패면 15분 잠금. 잠금이 끝난 뒤엔 1회부터 다시 센다 */
    fun recordLoginFailure(now: Instant) {
        val lockExpired = lockedUntil != null && !lockedUntil!!.isAfter(now)
        if (lockExpired) {
            loginFailCount = 0
            lockedUntil = null
        }

        loginFailCount += 1

        if (loginFailCount >= MAX_FAILURES) {
            lockedUntil = now.plus(LOCK_DURATION)
        }
    }

    fun recordLoginSuccess() {
        loginFailCount = 0
        lockedUntil = null
    }

    companion object {
        const val MAX_FAILURES = 5
        val LOCK_DURATION: Duration = Duration.ofMinutes(15)
    }
}
