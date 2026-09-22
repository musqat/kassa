package com.kassa.common.security

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.time.Duration
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@ConfigurationProperties("app.jwt")
data class JwtProperties(
    val secret: String,
    val ttl: Duration,
)

// HS256 대칭키
@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class JwtConfig(
    private val props: JwtProperties,
    private val userTokenValidator: UserTokenValidator,
) {
    private val key: SecretKey = SecretKeySpec(props.secret.toByteArray(), "HmacSHA256")

    @Bean
    fun jwtEncoder(): JwtEncoder =
        NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build()

    @Bean
    fun jwtDecoder(): JwtDecoder {
        val decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build()
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(JwtValidators.createDefault(), userTokenValidator),
        )
        return decoder
    }
}
