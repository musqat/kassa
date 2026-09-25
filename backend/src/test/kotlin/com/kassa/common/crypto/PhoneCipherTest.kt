package com.kassa.common.crypto

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.Base64
import javax.crypto.AEADBadTagException

class PhoneCipherTest {

    private val cipher = PhoneCipher(Base64.getEncoder().encodeToString("a".repeat(32).toByteArray()))

    @Test
    fun `암호화한 값을 다시 읽으면 원문이다`() {
        val encrypted = cipher.encrypt("010-1234-5678")

        assertThat(cipher.decrypt(encrypted)).isEqualTo("010-1234-5678")
    }

    @Test
    fun `같은 번호도 암호문이 매번 다르다`() {
        val first = cipher.encrypt("010-1234-5678")
        val second = cipher.encrypt("010-1234-5678")

        assertThat(first).isNotEqualTo(second)
        assertThat(cipher.decrypt(first)).isEqualTo(cipher.decrypt(second))
    }

    @Test
    fun `저장 값에 원문이 남지 않는다`() {
        assertThat(cipher.encrypt("010-1234-5678")).doesNotContain("1234", "5678")
    }

    @Test
    fun `형식이 아니면 예외`() {
        assertThatThrownBy { cipher.decrypt("이건암호문이아니다") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `한 글자라도 바뀌면 복호화되지 않는다`() {
        val encrypted = cipher.encrypt("010-1234-5678")
        val (iv, body) = encrypted.split(":")
        val bytes = Base64.getDecoder().decode(body)
        bytes[0] = (bytes[0].toInt() xor 0x01).toByte()
        val broken = "$iv:" + Base64.getEncoder().encodeToString(bytes)

        assertThatThrownBy { cipher.decrypt(broken) }
            .isInstanceOf(AEADBadTagException::class.java)
    }
}
