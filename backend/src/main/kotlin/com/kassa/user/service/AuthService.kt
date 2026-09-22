package com.kassa.user.service

import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import com.kassa.common.security.TokenIssuer
import com.kassa.user.domain.User
import com.kassa.user.dto.LoginRequest
import com.kassa.user.dto.TokenResponse
import com.kassa.user.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenIssuer: TokenIssuer,
    private val clock: Clock,
) {

    // 실패로 끝나도 실패 횟수·잠금은 커밋한다
    @Transactional(noRollbackFor = [BusinessException::class])
    fun login(request: LoginRequest): TokenResponse {
        val now = Instant.now(clock)

        val user = userRepository.findByLoginId(request.loginId.trim())
            ?: throw BusinessException(ErrorCode.LOGIN_ID_NOT_FOUND)

        if (user.isLocked(now)) throw locked(user, now)

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            user.recordLoginFailure(now)
            if (user.isLocked(now)) throw locked(user, now)
            throw BusinessException(
                ErrorCode.LOGIN_FAILED,
                "비밀번호가 맞지 않습니다. ${user.remainingAttempts()}회 더 틀리면 15분간 로그인이 제한됩니다",
            )
        }

        user.recordLoginSuccess()
        val token = tokenIssuer.issue(user.id!!, now)
        return TokenResponse(accessToken = token.value, expiresIn = token.expiresInSeconds)
    }

    private fun locked(user: User, now: Instant): BusinessException {
        val seconds = Duration.between(now, user.lockedUntil).seconds
        val minutes = (seconds + 59) / 60
        return BusinessException(ErrorCode.ACCOUNT_LOCKED, "로그인 시도가 많아 제한됐습니다. ${minutes}분 뒤 다시 시도하세요")
    }
}
