package com.kassa.common.security

import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Component
import java.time.Instant

data class IssuedToken(
    val value: String,
    val expiresInSeconds: Long,
)

// 토큰에는 회원 id 만 넣는다
@Component
class TokenIssuer(
    private val encoder: JwtEncoder,
    private val props: JwtProperties,
) {
    fun issue(userId: Long, now: Instant): IssuedToken {
        val claims = JwtClaimsSet.builder()
            .subject(userId.toString())
            .issuedAt(now)
            .expiresAt(now.plus(props.ttl))
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        val token = encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
        return IssuedToken(token, props.ttl.seconds)
    }
}
