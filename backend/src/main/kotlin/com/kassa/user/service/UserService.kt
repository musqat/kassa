package com.kassa.user.service

import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import com.kassa.user.domain.User
import com.kassa.user.dto.SignupRequest
import com.kassa.user.dto.UserResponse
import com.kassa.user.repository.EmailTokenRepository
import com.kassa.user.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailVerificationService: EmailVerificationService,
    private val emailTokenRepository: EmailTokenRepository,
    private val clock: Clock,
) {

    /** 인증된 회원의 이메일·아이디면 409. 미인증 회원이 쥔 이메일·아이디는 풀어 준다 */
    @Transactional
    fun signUp(request: SignupRequest) {
        val now = Instant.now(clock)
        val email = request.email.trim().lowercase()

        val existing = userRepository.findByEmail(email)

        if (existing != null && existing.isEmailVerified()) throw BusinessException(ErrorCode.EMAIL_DUPLICATED)

        val idOwner = userRepository.findByLoginId(request.loginId)
        if (idOwner != null && idOwner.id != existing?.id) {
            if (idOwner.isEmailVerified()) {
                throw BusinessException(ErrorCode.LOGIN_ID_DUPLICATED)
            }

            emailTokenRepository.deleteAllByUserId(idOwner.id!!)
            userRepository.delete(idOwner)

            userRepository.flush()
        }

        val passwordHash = passwordEncoder.encode(request.password)!!

        if (existing == null) {
            val savedUser = saveNew(User(request.loginId, email, passwordHash, request.name))
            emailVerificationService.sendVerification(savedUser, now)
        } else {
            existing.overwriteSignup(request.loginId, passwordHash, request.name)
            emailVerificationService.sendVerification(existing, now)
        }
    }

    // 동시에 같은 값으로 가입하면 유니크 제약에서 걸린다
    private fun saveNew(user: User): User =
        try {
            userRepository.saveAndFlush(user)
        } catch (e: DataIntegrityViolationException) {
            val code = if (e.message?.contains("uk_users_login_id") == true) {
                ErrorCode.LOGIN_ID_DUPLICATED
            } else {
                ErrorCode.EMAIL_DUPLICATED
            }
            throw BusinessException(code)
        }

    @Transactional(readOnly = true)
    fun isLoginIdAvailable(loginId: String): Boolean =
        !userRepository.existsByLoginIdAndEmailVerifiedAtIsNotNull(loginId)

    /** 비밀번호를 한 번 더 확인하고 개인정보를 지운다 */
    @Transactional
    fun withdraw(userId: Long, password: String) {
        val now = Instant.now(clock)

        val user = userRepository.findByIdOrNull(userId) ?: throw BusinessException(ErrorCode.UNAUTHORIZED)

        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw BusinessException(ErrorCode.PASSWORD_MISMATCH)
        }

        emailTokenRepository.deleteAllByUserId(userId)

        user.withdraw(now)
    }

    // 회원이 없으면 401
    @Transactional(readOnly = true)
    fun me(userId: Long): UserResponse {
        val user = userRepository.findByIdOrNull(userId) ?: throw BusinessException(ErrorCode.UNAUTHORIZED)
        return UserResponse.from(user)
    }
}
