package com.kassa.user.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class UserTest {

    private val t0: Instant = Instant.parse("2026-09-22T00:00:00Z")
    private val beforeUnlock = t0.plus(Duration.ofMinutes(15)).minusSeconds(1)   // 00:14:59
    private val unlockAt = t0.plus(Duration.ofMinutes(15))                       // 00:15:00
    private val later = t0.plus(Duration.ofMinutes(16))                          // 00:16:00


    private fun user() = User("A@Example.com", "hash", "홍길동")

    private fun User.failTimes(n: Int, at: Instant = t0) = repeat(n) { recordLoginFailure(at) }

    @Test
    fun `이메일은 소문자로 저장된다`() {
        assertThat(user().email).isEqualTo("a@example.com")
    }

    @Test
    fun `4회 실패는 잠기지 않는다`() {
        val u = user()

        u.failTimes(4)
        assertThat(u.isLocked(t0)).isFalse()
    }

    @Test
    fun `5회 실패하면 잠긴다`() {
        val u = user()

        u.failTimes(5)
        assertThat(u.isLocked(t0)).isTrue()
        assertThat(u.lockedUntil).isEqualTo(t0.plus(Duration.ofMinutes(15)))
    }

    @Test
    fun `잠긴 뒤 15분이 지나면 풀린다`() {
        val u = user()
        u.failTimes(5)

        assertThat(u.isLocked(beforeUnlock)).isTrue()
        assertThat(u.isLocked(unlockAt)).isFalse()
    }

    @Test
    fun `잠금이 풀린 뒤 한 번 틀리면 1회부터 다시 센다`() {
        val u = user()
        u.failTimes(5)
        u.failTimes(1, at = later)

        assertThat(u.loginFailCount).isEqualTo(1)
        assertThat(u.isLocked(later)).isFalse()
    }

    @Test
    fun `성공하면 실패 횟수와 잠금이 지워진다`() {
        val u = user()
        u.failTimes(3)
        u.recordLoginSuccess()
        assertThat(u.loginFailCount).isEqualTo(0)
        assertThat(u.lockedUntil).isNull()
    }
}
