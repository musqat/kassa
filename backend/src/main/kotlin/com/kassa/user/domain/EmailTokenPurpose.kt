package com.kassa.user.domain

import java.time.Duration

enum class EmailTokenPurpose(val ttl: Duration) {
    VERIFY_EMAIL(Duration.ofHours(24)),
    RESET_PASSWORD(Duration.ofMinutes(30)),

    // 링크 없는 메일이라 발송 기록으로만 쓴다
    FIND_LOGIN_ID(Duration.ZERO),
}
