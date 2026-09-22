package com.kassa.user.service

import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import com.kassa.user.domain.User
import com.kassa.user.dto.SignupRequest
import com.kassa.user.dto.UserResponse
import com.kassa.user.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {

    @Transactional
    fun signUp(request: SignupRequest): UserResponse {
        val email = request.email.trim().lowercase()
        if (userRepository.existsByLoginId(request.loginId)) throw BusinessException(ErrorCode.LOGIN_ID_DUPLICATED)
        if (userRepository.existsByEmail(email)) throw BusinessException(ErrorCode.EMAIL_DUPLICATED)

        val user = User(request.loginId, email, passwordEncoder.encode(request.password)!!, request.name)

        val saved = try {
            userRepository.saveAndFlush(user)
        } catch (e: DataIntegrityViolationException) {
            // 동시에 같은 값으로 가입하면 유니크 제약에서 걸린다
            val code = if (e.message?.contains("uk_users_login_id") == true) {
                ErrorCode.LOGIN_ID_DUPLICATED
            } else {
                ErrorCode.EMAIL_DUPLICATED
            }
            throw BusinessException(code)
        }
        return UserResponse.from(saved)
    }

    @Transactional(readOnly = true)
    fun isLoginIdAvailable(loginId: String): Boolean = !userRepository.existsByLoginId(loginId)

    // 회원이 없으면 401
    @Transactional(readOnly = true)
    fun me(userId: Long): UserResponse {
        val user = userRepository.findByIdOrNull(userId)?:
            throw BusinessException(ErrorCode.UNAUTHORIZED)
        return UserResponse.from(user)
    }
}
