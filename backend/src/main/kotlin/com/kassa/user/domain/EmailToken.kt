package com.kassa.user.domain

import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

// 원문은 메일 링크에만 있고 DB 에는 SHA-256 해시만 둔다
@Entity
@Table(name = "email_token")
class EmailToken(
    userId: Long,
    purpose: EmailTokenPurpose,
    tokenHash: String,
    now: Instant,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    var userId: Long = userId
        protected set

    @Enumerated(EnumType.STRING)
    var purpose: EmailTokenPurpose = purpose
        protected set

    var tokenHash: String = tokenHash
        protected set

    var expiresAt: Instant = now.plus(purpose.ttl)
        protected set

    var usedAt: Instant? = null
        protected set

    var createdAt: Instant = now
        protected set

    /** 안 썼고 만료 전 */
    fun isUsable(now: Instant): Boolean {
        return usedAt == null && expiresAt.isAfter(now)
    }

    fun use(now: Instant) {
        if (!isUsable(now)){
            throw BusinessException(ErrorCode.INVALID_EMAIL_TOKEN)
        }
        usedAt = now
    }
}
