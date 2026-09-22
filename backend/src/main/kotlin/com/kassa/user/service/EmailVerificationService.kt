package com.kassa.user.service

import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import com.kassa.user.domain.EmailTokenPurpose
import com.kassa.user.domain.User
import com.kassa.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class EmailVerificationService(
    private val userRepository: UserRepository,
    private val emailTokenService: EmailTokenService,
    private val accountMailer: AccountMailer,
    private val clock: Clock,
) {

    /** 발송 제한에 걸리면 보내지 않는다. 가입과 재전송이 같이 쓴다 */
    @Transactional
    fun sendVerification(user: User, now: Instant) {
        if (emailTokenService.isRateLimited(user.id!!, EmailTokenPurpose.VERIFY_EMAIL, now)) return

        val rawToken = emailTokenService.issue(user.id!!, EmailTokenPurpose.VERIFY_EMAIL, now)

        accountMailer.sendVerification(user.email, rawToken)
    }

    /** 응답은 항상 같다. 미인증 회원에게만 보낸다 */
    @Transactional
    fun resend(email: String) {
        val now = Instant.now(clock)
        val user = userRepository.findByEmail(email.trim().lowercase())?: return

        if (user.isEmailVerified()){
            return
        }

        sendVerification(user, now)
    }

    @Transactional
    fun confirm(rawToken: String) {
        val now = Instant.now(clock)
        val id = emailTokenService.consume(rawToken, EmailTokenPurpose.VERIFY_EMAIL, now)

        val user = userRepository.findByIdOrNull(id) ?: throw BusinessException(ErrorCode.INVALID_EMAIL_TOKEN)

        user.verifyEmail(now)
    }
}
