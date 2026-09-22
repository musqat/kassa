package com.kassa.user.controller

import com.kassa.user.dto.EmailRequest
import com.kassa.user.dto.EmailTokenRequest
import com.kassa.user.dto.LoginRequest
import com.kassa.user.dto.PasswordResetRequest
import com.kassa.user.dto.TokenResponse
import com.kassa.user.service.AccountRecoveryService
import com.kassa.user.service.AuthService
import com.kassa.user.service.EmailVerificationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "인증")
@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
    private val emailVerificationService: EmailVerificationService,
    private val accountRecoveryService: AccountRecoveryService,
) {

    @Operation(summary = "로그인", description = "없는 아이디 401 USER_006, 틀린 비밀번호 401 USER_002, 잠금 423 USER_007, 미인증 403 USER_003")
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): TokenResponse =
        authService.login(request)

    @Operation(summary = "인증 메일 재전송", description = "응답은 항상 202")
    @PostMapping("/email-verification")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun resendVerification(@Valid @RequestBody request: EmailRequest) {
        emailVerificationService.resend(request.email)
    }

    @Operation(summary = "이메일 인증 확인")
    @PostMapping("/email-verification/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun confirmVerification(@Valid @RequestBody request: EmailTokenRequest) {
        emailVerificationService.confirm(request.token)
    }

    @Operation(summary = "아이디 찾기", description = "응답은 항상 202")
    @PostMapping("/login-id/find")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun findLoginId(@Valid @RequestBody request: EmailRequest) {
        accountRecoveryService.sendLoginId(request.email)
    }

    @Operation(summary = "비밀번호 재설정 메일 요청", description = "응답은 항상 202")
    @PostMapping("/password-reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun requestPasswordReset(@Valid @RequestBody request: EmailRequest) {
        accountRecoveryService.sendPasswordReset(request.email)
    }

    @Operation(summary = "비밀번호 재설정")
    @PostMapping("/password-reset/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun resetPassword(@Valid @RequestBody request: PasswordResetRequest) {
        accountRecoveryService.resetPassword(request.token, request.newPassword)
    }
}
