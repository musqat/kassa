package com.kassa.common.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig(
    private val entryPoint: ProblemAuthenticationEntryPoint,
    private val deniedHandler: ProblemAccessDeniedHandler,
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            cors { }
            csrf { disable() }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            authorizeHttpRequests {
                authorize(HttpMethod.GET, "/api/products/**", permitAll)
                authorize(HttpMethod.POST, "/api/users", permitAll)
                authorize(HttpMethod.POST, "/api/auth/login", permitAll)
                authorize("/actuator/health/**", permitAll)
                authorize(anyRequest, authenticated)
            }
            oauth2ResourceServer {
                jwt { }
                // 잘못된·만료된 토큰은 이쪽 진입점으로 온다
                authenticationEntryPoint = entryPoint
            }
            exceptionHandling {
                // 토큰이 아예 없는 요청은 이쪽으로 온다
                authenticationEntryPoint = entryPoint
                accessDeniedHandler = deniedHandler
            }
        }
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
