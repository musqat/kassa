package com.kassa.user.service

import com.kassa.user.domain.EmailTokenPurpose
import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import com.kassa.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class AccountRecoveryService(
    private val userRepository: UserRepository,
    private val emailTokenService: EmailTokenService,
    private val accountMailer: AccountMailer,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock,
) {

    /** 응답은 항상 같다. 인증된 회원에게만 보낸다 */
    @Transactional
    fun sendLoginId(email: String) {
        val now = Instant.now(clock)
        val user = userRepository.findByEmail(email.trim().lowercase()) ?: return

        if (!user.isEmailVerified()) {
            return
        }

        if (emailTokenService.isRateLimited(user.id!!, EmailTokenPurpose.FIND_LOGIN_ID, now)) {
            return
        }

        emailTokenService.issue(user.id!!, EmailTokenPurpose.FIND_LOGIN_ID, now)

        accountMailer.sendLoginId(user.email, user.loginId)
    }

    /** 응답은 항상 같다. 회원이 있으면 인증 여부와 상관없이 보낸다 */
    @Transactional
    fun sendPasswordReset(email: String) {
        val now = Instant.now(clock)
        val user = userRepository.findByEmail(email.trim().lowercase()) ?: return
        if (emailTokenService.isRateLimited(user.id!!, EmailTokenPurpose.RESET_PASSWORD, now)) {
            return
        }

        val rawToken = emailTokenService.issue(user.id!!, EmailTokenPurpose.RESET_PASSWORD ,now)

        accountMailer.sendPasswordReset(user.email, rawToken)
    }

    @Transactional
    fun resetPassword(rawToken: String, newPassword: String) {
        val now = Instant.now(clock)
        val userId = emailTokenService.consume(rawToken, EmailTokenPurpose.RESET_PASSWORD, now)

        val user = userRepository.findByIdOrNull(userId) ?: throw BusinessException(ErrorCode.INVALID_EMAIL_TOKEN)

        user.resetPassword(passwordEncoder.encode(newPassword)!!, now)

    }
}
