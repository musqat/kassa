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
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "CATALOG_001", "상품을 찾을 수 없습니다"),

    // 회원 에러
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "USER_001", "이미 가입된 이메일입니다"),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "USER_002", "비밀번호가 맞지 않습니다"),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "USER_003", "이메일 인증이 필요합니다"),
    INVALID_EMAIL_TOKEN(HttpStatus.BAD_REQUEST, "USER_004", "링크가 만료됐거나 이미 사용됐습니다"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "USER_005", "비밀번호가 맞지 않습니다"),
    LOGIN_ID_NOT_FOUND(HttpStatus.UNAUTHORIZED, "USER_006", "존재하지 않는 아이디입니다"),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "USER_007", "로그인 시도가 많아 잠시 제한됩니다"),
    LOGIN_ID_DUPLICATED(HttpStatus.CONFLICT, "USER_008", "이미 사용 중인 아이디입니다"),

    // 메일 에러
    MAIL_SEND_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "MAIL_001", "메일을 보내지 못했습니다. 잠시 뒤 다시 시도하세요"),

    // 장바구니 에러
    CART_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "CART_001", "장바구니에 없는 상품입니다"),
    CART_QUANTITY_EXCEEDED(HttpStatus.BAD_REQUEST, "CART_002", "한 상품은 99개까지 담을 수 있습니다"),
    PRODUCT_NOT_ORDERABLE(HttpStatus.CONFLICT, "CART_003", "지금은 담을 수 없는 상품입니다"),

    // 인증 에러
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_001", "로그인이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_002", "권한이 없습니다");

}
