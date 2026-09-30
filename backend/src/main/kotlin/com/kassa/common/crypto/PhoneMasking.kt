package com.kassa.common.crypto

// 010-1234-5678 을 010-****-5678 로
fun maskPhone(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    if (digits.length < 8) return "***"

    return digits.take(3) + "-****-" + digits.takeLast(4)
}
