package com.kassa.common.error

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

/**
 * 에러 응답은 RFC 9457 ProblemDetail 로 통일한다.
 * code·requestId 는 확장 프로퍼티로 얹는다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * 예상된 실패
     */
    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(e: BusinessException): ProblemDetail {
        log.warn("{} {}", e.errorCode.code, e.message)
        return problemOf(e.errorCode, e.message)
    }

    /**
     * @Valid 검증 실패. 어느 필드가 틀렸는지 detail 에 담으면 쓰기 편하다.
     * 필드 정보는 e.bindingResult.fieldErrors 에 있다.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ProblemDetail {
        val detail = e.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        log.warn("검증 실패 {}", detail)
        return problemOf(ErrorCode.INVALID_REQUEST, detail.ifEmpty { ErrorCode.INVALID_REQUEST.message })
    }

    /**
     * 예상하지 못한 실패. 여기서만 스택 트레이스를 남긴다.
     */
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ProblemDetail {
        log.error("처리하지 못한 예외", e)
        return problemOf(ErrorCode.INTERNAL_ERROR)
    }

    /**
     * ErrorCode 를 ProblemDetail 로 옮기는 공통 부분.
     * 세 핸들러가 같은 모양을 만들도록 한 곳에 둔다.
     */
    private fun problemOf(errorCode: ErrorCode, detail: String = errorCode.message): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(errorCode.status, detail)
        problem.setProperty("code", errorCode.code)
        problem.setProperty("requestId", MDC.get("requestId"))
        return problem
    }

}
