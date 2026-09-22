package com.kassa.user.repository

import com.kassa.user.domain.EmailToken
import com.kassa.user.domain.EmailTokenPurpose
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface EmailTokenRepository : JpaRepository<EmailToken, Long> {

    fun findByTokenHash(tokenHash: String): EmailToken?

    fun findAllByUserIdAndPurposeAndUsedAtIsNull(userId: Long, purpose: EmailTokenPurpose): List<EmailToken>

    fun findFirstByUserIdAndPurposeOrderByCreatedAtDesc(userId: Long, purpose: EmailTokenPurpose): EmailToken?

    fun deleteAllByUserId(userId: Long)

    fun countByUserIdAndPurposeAndCreatedAtAfter(userId: Long, purpose: EmailTokenPurpose, after: Instant): Long
}
