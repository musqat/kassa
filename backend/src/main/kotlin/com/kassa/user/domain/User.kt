package com.kassa.user.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

@Entity
@Table(name = "users")
class User(
    loginId: String,
    email: String,
    passwordHash: String,
    name: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    var loginId: String = loginId
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

    var emailVerifiedAt: Instant? = null
        protected set

    var tokenValidAfter: Instant? = null
        protected set

    var deletedAt: Instant? = null
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

    fun remainingAttempts(): Int = MAX_FAILURES - loginFailCount

    fun recordLoginSuccess() {
        loginFailCount = 0
        lockedUntil = null
    }

    fun isEmailVerified(): Boolean {
        return emailVerifiedAt != null
    }

    fun verifyEmail(now: Instant) {
        if (emailVerifiedAt == null) {
            emailVerifiedAt = now
        }
    }

    /** 미인증 상태에서 다시 가입하면 아이디·비밀번호·이름을 새 값으로 바꾼다 */
    fun overwriteSignup(loginId: String, passwordHash: String, name: String) {
        this.loginId = loginId
        this.passwordHash = passwordHash
        this.name = name
    }

    /** 재설정 링크로 비밀번호를 바꾼다 */
    fun resetPassword(passwordHash: String, now: Instant) {
        this.passwordHash = passwordHash
        recordLoginSuccess()
        verifyEmail(now)
        tokenValidAfter = now.truncatedTo(ChronoUnit.SECONDS)
    }

    companion object {
        const val MAX_FAILURES = 5
        val LOCK_DURATION: Duration = Duration.ofMinutes(15)
    }
}
