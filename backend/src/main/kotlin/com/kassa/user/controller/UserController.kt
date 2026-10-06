package com.kassa.user.controller

import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import com.kassa.common.security.userId
import com.kassa.user.dto.LOGIN_ID_MESSAGE
import com.kassa.user.dto.LOGIN_ID_REGEX
import com.kassa.user.dto.LoginIdAvailabilityResponse
import com.kassa.user.dto.ChangeNameRequest
import com.kassa.user.dto.ChangePasswordRequest
import com.kassa.user.dto.SignupRequest
import com.kassa.user.dto.WithdrawRequest
import com.kassa.user.dto.UserResponse
import com.kassa.user.service.SignupRateLimiter
import com.kassa.user.service.UserService
import jakarta.servlet.http.HttpServletRequest
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
import org.springframework.web.bind.annotation.PatchMapping
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
    private val signupRateLimiter: SignupRateLimiter,
) {

    @Operation(summary = "회원가입", description = "인증 메일을 보낸다. 인증된 이메일·아이디면 409, 같은 IP 가 짧은 시간에 많이 가입하면 429")
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun signUp(@Valid @RequestBody request: SignupRequest, httpRequest: HttpServletRequest) {
        // Fly 프록시가 실제 클라이언트 IP 를 이 헤더에 넣는다. 로컬은 접속한 주소를 쓴다
        val ip = httpRequest.getHeader("Fly-Client-IP") ?: httpRequest.remoteAddr
        if (!signupRateLimiter.tryAcquire(ip)) {
            throw BusinessException(ErrorCode.TOO_MANY_REQUESTS)
        }
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

    @Operation(summary = "이름 변경")
    @SecurityRequirement(name = "bearer-jwt")
    @PatchMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changeName(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: ChangeNameRequest) {
        userService.changeName(jwt.userId(), request.name)
    }

    @Operation(summary = "비밀번호 변경", description = "지금 비밀번호를 확인한다. 바꾸면 모든 기기의 토큰이 무효가 된다")
    @SecurityRequirement(name = "bearer-jwt")
    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun changePassword(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: ChangePasswordRequest) {
        userService.changePassword(jwt.userId(), request.currentPassword, request.newPassword)
    }

    @Operation(summary = "탈퇴", description = "비밀번호를 한 번 더 확인한다")
    @SecurityRequirement(name = "bearer-jwt")
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun withdraw(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: WithdrawRequest) {
        userService.withdraw(jwt.userId(), request.password)
    }
}
