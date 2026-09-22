package com.kassa.common.security

import com.kassa.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

// 서명·만료가 맞아도 회원 쪽 사정으로 막아야 하는 토큰을 거른다
@Component
class UserTokenValidator(
    private val userRepository: UserRepository,
) : OAuth2TokenValidator<Jwt> {

    override fun validate(token: Jwt): OAuth2TokenValidatorResult {
        val userId = token.subject?.toLongOrNull() ?: return fail()
        val user = userRepository.findByIdOrNull(userId) ?: return fail()

        if (user.deletedAt != null) return fail()

        val validAfter = user.tokenValidAfter

        if (validAfter != null && token.issuedAt?.isBefore(validAfter) == true) {
            return fail()
        }

        return OAuth2TokenValidatorResult.success()
    }

    private fun fail(): OAuth2TokenValidatorResult =
        OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token"))
}
