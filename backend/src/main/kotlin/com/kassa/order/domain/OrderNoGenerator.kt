package com.kassa.order.domain

import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// 20260924-7F3A9C2B 모양. 포트원 paymentId 로도 쓰여서 영문·숫자·하이픈만 쓴다
@Component
class OrderNoGenerator {

    private val random = SecureRandom()

    fun generate(now: Instant): String {
        val date = DATE_FORMAT.format(now.atZone(KST))

        val suffix = (1..SUFFIX_LENGTH)
            .map { ALPHABET[random.nextInt(ALPHABET.length)] }
            .joinToString("")

        return date + "-" + suffix
    }

    private companion object {
        val KST: ZoneId = ZoneId.of("Asia/Seoul")
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        // 헷갈리는 0·O·1·I 는 뺀다
        const val ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        const val SUFFIX_LENGTH = 8
    }
}
