package com.kassa.user

import com.kassa.support.MutableClock
import com.kassa.user.service.SignupRateLimiter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration

class SignupRateLimiterTest {

    private val clock = MutableClock()
    private val limiter = SignupRateLimiter(max = 3, window = Duration.ofMinutes(10), clock = clock)

    @Test
    fun `같은 IP 는 창 안에서 max 번까지만 받는다`() {
        repeat(3) { assertThat(limiter.tryAcquire("1.1.1.1")).isTrue() }

        assertThat(limiter.tryAcquire("1.1.1.1")).isFalse()
    }

    @Test
    fun `다른 IP 는 따로 센다`() {
        repeat(3) { limiter.tryAcquire("1.1.1.1") }

        assertThat(limiter.tryAcquire("2.2.2.2")).isTrue()
    }

    @Test
    fun `창이 지나면 다시 받는다`() {
        repeat(3) { limiter.tryAcquire("1.1.1.1") }

        clock.advance(Duration.ofMinutes(10).plusSeconds(1))

        assertThat(limiter.tryAcquire("1.1.1.1")).isTrue()
    }

    @Test
    fun `거절한 시도는 세지 않는다`() {
        repeat(3) { limiter.tryAcquire("1.1.1.1") }
        clock.advance(Duration.ofMinutes(5))
        repeat(5) { limiter.tryAcquire("1.1.1.1") }

        // 처음 셋이 빠지면 다시 받는다. 거절된 다섯 번이 남아 있으면 계속 막힌다
        clock.advance(Duration.ofMinutes(5).plusSeconds(1))

        assertThat(limiter.tryAcquire("1.1.1.1")).isTrue()
    }

    @Test
    fun `창이 지난 IP 는 맵이 커지면 지워진다`() {
        repeat(SignupRateLimiter.SWEEP_THRESHOLD + 1) { limiter.tryAcquire("10.0.$it") }
        clock.advance(Duration.ofMinutes(11))

        // 훑기는 다음 가입에서 돈다
        limiter.tryAcquire("9.9.9.9")

        assertThat(limiter.trackedIps()).isEqualTo(1)
    }
}
