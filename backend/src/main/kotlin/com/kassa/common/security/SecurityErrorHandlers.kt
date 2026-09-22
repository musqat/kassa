package com.kassa.common.security

import com.kassa.common.error.BusinessException
import com.kassa.common.error.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver

// 인증·인가 실패를 MVC 예외 처리기로 넘긴다
@Component
class ProblemAuthenticationEntryPoint(
    @Qualifier("handlerExceptionResolver") private val resolver: HandlerExceptionResolver,
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        resolver.resolveException(request, response, null, BusinessException(ErrorCode.UNAUTHORIZED))
    }
}

@Component
class ProblemAccessDeniedHandler(
    @Qualifier("handlerExceptionResolver") private val resolver: HandlerExceptionResolver,
) : AccessDeniedHandler {

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        resolver.resolveException(request, response, null, BusinessException(ErrorCode.FORBIDDEN))
    }
}
