package com.kassa.common.trace

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

const val REQUEST_ID = "requestId"
const val REQUEST_ID_HEADER = "X-Request-Id"

/**
 * 요청마다 식별자를 만들어 MDC 와 응답 헤더에 넣는다.
 *
 * 로그 패턴의 %X{requestId} 가 이 값을 찍고, 에러 응답의 requestId 도 같은 값이다.
 * 사용자가 오류를 알려오면 그 값으로 로그를 찾는다.
 *
 * orderNo 와 sagaId 가 같은 방식으로 더해진다.
 * 사가는 단계마다 트랜잭션이 끊겨 로그가 흩어지기 때문에 이 필터가 필요하다.
 */
@Component
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
