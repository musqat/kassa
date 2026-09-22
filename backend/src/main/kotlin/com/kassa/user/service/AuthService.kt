package com.kassa.user.service

import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import com.kassa.common.security.TokenIssuer
import com.kassa.user.dto.LoginRequest
import com.kassa.user.dto.TokenResponse
import com.kassa.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokenIssuer: TokenIssuer,
    private val clock: Clock,
) {

    // 없는 이메일도 같은 시간이 걸리게 비교할 가짜 해시
    private val dummyHash: String = passwordEncoder.encode("dummy-password-for-timing")!!

    // 실패로 끝나도 실패 횟수·잠금은 커밋한다
    @Transactional(noRollbackFor = [BusinessException::class])
    fun login(request: LoginRequest): TokenResponse {
        val now = Instant.now(clock)

        val user = userRepository.findByEmail(request.email.trim().lowercase())
        if (user == null) {
            passwordEncoder.matches(request.password, dummyHash)
            throw BusinessException(ErrorCode.LOGIN_FAILED)
        }

        // 잠겨 있어도 틀린 비밀번호와 같은 응답·같은 시간
        if (user.isLocked(now)) {
            passwordEncoder.matches(request.password, user.passwordHash)
            throw BusinessException(ErrorCode.LOGIN_FAILED)
        }

        if (!passwordEncoder.matches(request.password, user.passwordHash)){
            user.recordLoginFailure(now)
            throw BusinessException(ErrorCode.LOGIN_FAILED)
        }

        user.recordLoginSuccess()
        val token = tokenIssuer.issue(user.id!!, now)
        return TokenResponse(accessToken = token.value, expiresIn = token.expiresInSeconds)

    }
}
