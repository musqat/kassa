package com.kassa.user.controller

import com.kassa.user.dto.EmailRequest
import com.kassa.user.dto.EmailTokenRequest
import com.kassa.user.dto.LoginRequest
import com.kassa.user.dto.PasswordResetRequest
import com.kassa.user.dto.TokenResponse
import com.kassa.user.service.AccountRecoveryService
import com.kassa.user.service.AuthService
import com.kassa.user.service.EmailVerificationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val emailVerificationService: EmailVerificationService,
    private val accountRecoveryService: AccountRecoveryService,
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): TokenResponse =
        authService.login(request)

    @PostMapping("/email-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun resendVerification(@Valid @RequestBody request: EmailRequest) {
        emailVerificationService.resend(request.email)
    }

    @PostMapping("/email-verification/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun confirmVerification(@Valid @RequestBody request: EmailTokenRequest) {
        emailVerificationService.confirm(request.token)
    }

    @PostMapping("/login-id/find")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun findLoginId(@Valid @RequestBody request: EmailRequest) {
        accountRecoveryService.sendLoginId(request.email)
    }

    @PostMapping("/password-reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun requestPasswordReset(@Valid @RequestBody request: EmailRequest) {
        accountRecoveryService.sendPasswordReset(request.email)
    }

    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun resetPassword(@Valid @RequestBody request: PasswordResetRequest) {
        accountRecoveryService.resetPassword(request.token, request.newPassword)
    }
}
