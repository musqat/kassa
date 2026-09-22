package com.kassa.user.controller

import com.kassa.common.security.userId
import com.kassa.user.dto.LOGIN_ID_MESSAGE
import com.kassa.user.dto.LOGIN_ID_REGEX
import com.kassa.user.dto.LoginIdAvailabilityResponse
import com.kassa.user.dto.SignupRequest
import com.kassa.user.dto.WithdrawRequest
import com.kassa.user.dto.UserResponse
import com.kassa.user.service.UserService
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "회원")
@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
) {

    @Operation(summary = "회원가입", description = "인증 메일을 보낸다. 인증된 이메일·아이디면 409")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun signUp(@Valid @RequestBody request: SignupRequest) {
        userService.signUp(request)
    }

    @Operation(summary = "아이디 중복확인")
    @GetMapping("/login-id-check")
    fun loginIdAvailability(
        @RequestParam @Pattern(regexp = LOGIN_ID_REGEX, message = LOGIN_ID_MESSAGE) loginId: String,
    ): LoginIdAvailabilityResponse =
        LoginIdAvailabilityResponse(userService.isLoginIdAvailable(loginId))

    @Operation(summary = "내 정보")
    @SecurityRequirement(name = "bearer-jwt")
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal jwt: Jwt): UserResponse =
        userService.me(jwt.userId())

    @Operation(summary = "탈퇴", description = "비밀번호를 한 번 더 확인한다")
    @SecurityRequirement(name = "bearer-jwt")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun withdraw(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: WithdrawRequest) {
        userService.withdraw(jwt.userId(), request.password)
    }
}
