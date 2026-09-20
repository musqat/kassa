package com.kassa.common.error

/**
 * 예상된 실패를 나타내는 예외.
 *
 * message 를 넘기지 않으면 ErrorCode 의 기본 메시지를 쓴다.
 */
class BusinessException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
) : RuntimeException(message)
