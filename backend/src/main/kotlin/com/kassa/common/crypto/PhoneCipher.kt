package com.kassa.common.crypto

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// 저장 형식은 Base64(iv):Base64(암호문)
@Component
class PhoneCipher(
    @Value("\${app.crypto.phone-key}") key: String,
) {
    private val secretKey = SecretKeySpec(Base64.getDecoder().decode(key), "AES")
    private val random = SecureRandom()

    fun encrypt(plain: String): String {
        val iv = ByteArray(IV_LENGTH)
        random.nextBytes(iv)

        val encrypted = cipher(Cipher.ENCRYPT_MODE, iv).doFinal(plain.toByteArray())
        val encoder = Base64.getEncoder()

        return encoder.encodeToString(iv) + ":" + encoder.encodeToString(encrypted)
    }

    fun decrypt(encrypted: String): String {
        val parts = encrypted.split(":")
        require(parts.size == 2) { "암호문 형식이 아닙니다" }

        val decoder = Base64.getDecoder()
        val iv = decoder.decode(parts[0])
        val bytes = decoder.decode(parts[1])

        return String(cipher(Cipher.DECRYPT_MODE, iv).doFinal(bytes))

    }

    private fun cipher(mode: Int, iv: ByteArray): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, secretKey, GCMParameterSpec(TAG_BITS, iv))
        }

    private companion object {
        const val IV_LENGTH = 12
        const val TAG_BITS = 128
    }
}
