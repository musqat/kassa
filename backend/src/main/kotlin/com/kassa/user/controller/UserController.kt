package com.kassa.user.controller

import com.kassa.common.security.userId
import com.kassa.user.dto.LOGIN_ID_MESSAGE
import com.kassa.user.dto.LOGIN_ID_REGEX
import com.kassa.user.dto.LoginIdAvailabilityResponse
import com.kassa.user.dto.SignupRequest
import com.kassa.user.dto.UserResponse
import com.kassa.user.service.UserService
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun signUp(@Valid @RequestBody request: SignupRequest) {
        userService.signUp(request)
    }

    @GetMapping("/login-id-check")
    fun loginIdAvailability(
        @RequestParam @Pattern(regexp = LOGIN_ID_REGEX, message = LOGIN_ID_MESSAGE) loginId: String,
    ): LoginIdAvailabilityResponse =
        LoginIdAvailabilityResponse(userService.isLoginIdAvailable(loginId))

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal jwt: Jwt): UserResponse =
        userService.me(jwt.userId())
}
