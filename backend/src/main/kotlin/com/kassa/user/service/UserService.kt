package com.kassa.user.service

import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import com.kassa.user.domain.User
import com.kassa.user.dto.SignupRequest
import com.kassa.user.dto.UserResponse
import com.kassa.user.repository.UserRepository
import org.springframework.dao.DataIntegrityViolationException
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
        if (userRepository.existsByEmail(email)) throw BusinessException(ErrorCode.EMAIL_DUPLICATED)

        val user = User(email, passwordEncoder.encode(request.password)!!, request.name)

        val saved = try{
            userRepository.saveAndFlush(user)
        } catch (e : DataIntegrityViolationException){
            throw BusinessException(ErrorCode.EMAIL_DUPLICATED)
        }
        return UserResponse.from(saved)
    }
}
