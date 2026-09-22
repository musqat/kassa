package com.kassa.common.trace

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

const val REQUEST_ID = "requestId"
const val REQUEST_ID_HEADER = "X-Request-Id"

// 요청마다 식별자를 만들어 MDC 와 응답 헤더에 넣는다
// 시큐리티 필터보다 먼저 실행
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestTraceFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val uuid = UUID.randomUUID().toString().substring(0,8)

        MDC.put(REQUEST_ID, uuid)
        response.setHeader(REQUEST_ID_HEADER, uuid)

        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.clear()
        }
    }
}
