package com.kassa.common.error

import com.kassa.common.trace.REQUEST_ID
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.ProblemDetail
import org.springframework.mail.MailException
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import kotlin.text.ifEmpty

const val CODE = "code"

private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

/**
 * 에러 응답은 RFC 9457 ProblemDetail 로 통일한다.
 * code·requestId 는 확장 프로퍼티로 얹는다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    /** 예상된 실패 */
    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(e: BusinessException): ProblemDetail {
        log.warn("{} {}", e.errorCode.code, e.message)
        return problemOf(e.errorCode, e.message)
    }

    /** @Valid 검증 실패 */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ProblemDetail {
        val detail = e.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        log.warn("검증 실패 {}", detail)
        return problemOf(ErrorCode.INVALID_REQUEST, detail.ifEmpty { ErrorCode.INVALID_REQUEST.message })
    }

    /** 요청 파라미터 검증 실패 */
    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleParameterValidation(e: HandlerMethodValidationException): ProblemDetail {
        val detail = e.parameterValidationResults
            .flatMap { result -> result.resolvableErrors.map { "${result.methodParameter.parameterName}: ${it.defaultMessage}" } }
            .joinToString(", ")
        log.warn("검증 실패 {}", detail)
        return problemOf(ErrorCode.INVALID_REQUEST, detail.ifEmpty { ErrorCode.INVALID_REQUEST.message })
    }


    /** 메일 서버 연결·인증 실패 */
    @ExceptionHandler(MailException::class)
    fun handleMail(e: MailException): ProblemDetail {
        log.error("메일 발송 실패", e)

        return problemOf(ErrorCode.MAIL_SEND_FAILED)
    }

    /**
     * 예상하지 못한 실패. 스택 트레이스를 남긴다.
     */
    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ProblemDetail {
        if (e is ErrorResponse) {
            log.warn("{} {}", e.statusCode, e.message)
            return e.body.apply {
                setProperty(CODE, "COMMON_${e.statusCode.value()}")
                setProperty(REQUEST_ID, MDC.get(REQUEST_ID))
            }
        }

        log.error("처리하지 못한 예외", e)
        return problemOf(ErrorCode.INTERNAL_ERROR)
    }

    /** ErrorCode 를 ProblemDetail 로 옮긴다 */
    private fun problemOf(errorCode: ErrorCode, detail: String = errorCode.message): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(errorCode.status, detail)
        problem.setProperty(CODE, errorCode.code)
        problem.setProperty(REQUEST_ID, MDC.get(REQUEST_ID))
        return problem
    }

}
