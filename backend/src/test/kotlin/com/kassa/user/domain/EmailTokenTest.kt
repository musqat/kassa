package com.kassa.user.domain

import com.kassa.common.error.BusinessException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class EmailTokenTest {

    private val t0: Instant = Instant.parse("2026-09-22T00:00:00Z")

    private fun token(purpose: EmailTokenPurpose = EmailTokenPurpose.VERIFY_EMAIL) =
        EmailToken(userId = 1L, purpose = purpose, tokenHash = "a".repeat(64), now = t0)

    @Test
    fun `인증 토큰은 24시간, 재설정 토큰은 30분 뒤 만료된다`() {
        assertThat(token(EmailTokenPurpose.VERIFY_EMAIL).expiresAt).isEqualTo(t0.plus(Duration.ofHours(24)))
        assertThat(token(EmailTokenPurpose.RESET_PASSWORD).expiresAt).isEqualTo(t0.plus(Duration.ofMinutes(30)))
    }

    @Test
    fun `만료 전이고 안 썼으면 쓸 수 있다`() {
        val t = token()

        assertThat(t.isUsable(t0)).isTrue
    }

    @Test
    fun `만료 시각이 되면 쓸 수 없다`() {
        val t = token()
        val expiredAt = t.expiresAt

        assertThat(t.isUsable(expiredAt.minusSeconds(1))).isTrue
        assertThat(t.isUsable(expiredAt)).isFalse
    }

    @Test
    fun `한 번 쓰면 다시 쓸 수 없다`() {
        val t = token()
        t.use(t0)
        assertThat(t.usedAt).isEqualTo(t0)
        assertThat(t.isUsable(t0)).isFalse
        assertThatThrownBy { t.use(t0) }.isInstanceOf(BusinessException::class.java)
    }

    @Test
    fun `만료된 토큰은 쓸 수 없다`() {
        val t = token()
        assertThatThrownBy { t.use(t0.plus(Duration.ofHours(25))) }.isInstanceOf(BusinessException::class.java)
    }
}
