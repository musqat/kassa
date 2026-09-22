package com.kassa.user.service

import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import com.kassa.user.domain.EmailToken
import com.kassa.user.domain.EmailTokenPurpose
import com.kassa.user.repository.EmailTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.HexFormat

@Service
class EmailTokenService(
    private val emailTokenRepository: EmailTokenRepository,
) {
    private val random = SecureRandom()

    /** 원문을 돌려준다. 같은 회원·같은 목적의 안 쓴 토큰은 모두 사용 처리한다 */
    @Transactional
    fun issue(userId: Long, purpose: EmailTokenPurpose, now: Instant): String {
        val emailTokens =
            emailTokenRepository.findAllByUserIdAndPurposeAndUsedAtIsNull(userId, purpose)

        for (token in emailTokens){
            if (token.isUsable(now)){
                token.use(now)
            }
        }

        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        val raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val hash = hash(raw)

        val token = EmailToken(userId, purpose, hash, now)
        emailTokenRepository.save(token)

        return raw
    }

    /** 토큰을 쓰고 회원 id 를 돌려준다. 이유와 상관없이 실패는 모두 INVALID_EMAIL_TOKEN */
    @Transactional
    fun consume(raw: String, purpose: EmailTokenPurpose, now: Instant): Long {
        val token =emailTokenRepository.findByTokenHash(hash(raw))?: throw BusinessException(ErrorCode.INVALID_EMAIL_TOKEN)

        if(token.purpose != purpose){
            throw BusinessException(ErrorCode.INVALID_EMAIL_TOKEN)
        }
        token.use(now)

        return token.userId
    }

    /** 1분 안에 발급했거나 1시간 안에 5개를 발급했으면 true */
    @Transactional(readOnly = true)
    fun isRateLimited(userId: Long, purpose: EmailTokenPurpose, now: Instant): Boolean {
        val last = emailTokenRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(userId, purpose) ?: return false
        if (last.createdAt.plus(RESEND_INTERVAL).isAfter(now)) return true

        val issuedLastHour =
            emailTokenRepository.countByUserIdAndPurposeAndCreatedAtAfter(userId, purpose, now.minus(HOURLY_WINDOW))

        return issuedLastHour >= HOURLY_LIMIT
    }

    private fun hash(raw: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return HexFormat.of().formatHex(digest)
    }

    companion object {
        val RESEND_INTERVAL: Duration = Duration.ofMinutes(1)
        val HOURLY_WINDOW: Duration = Duration.ofHours(1)
        const val HOURLY_LIMIT = 5
    }
}
