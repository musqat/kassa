package com.kassa.user.domain

import java.time.Duration

enum class EmailTokenPurpose(val ttl: Duration) {
    VERIFY_EMAIL(Duration.ofHours(24)),
    RESET_PASSWORD(Duration.ofMinutes(30)),
}
