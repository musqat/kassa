package com.kassa.common.error

import org.springframework.http.HttpStatus

/**
 * 에러 코드 체계. 접두사는 도메인 이름으로 둔다.
 * 스프링 예외는 COMMON_상태값 으로 GlobalExceptionHandler 에서 만든다.
 */
enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String,
) {
    // 공통 에러
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON_001", "요청이 올바르지 않습니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_999", "서버 오류가 발생했습니다"),

    // 카탈로그 에러
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "CATALOG_001", "상품을 찾을 수 없습니다");

}
