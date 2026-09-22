package com.kassa.user.repository

import com.kassa.user.domain.EmailToken
import com.kassa.user.domain.EmailTokenPurpose
import org.springframework.data.jpa.repository.JpaRepository

interface EmailTokenRepository : JpaRepository<EmailToken, Long> {

    fun findByTokenHash(tokenHash: String): EmailToken?

    fun findAllByUserIdAndPurposeAndUsedAtIsNull(userId: Long, purpose: EmailTokenPurpose): List<EmailToken>
}
