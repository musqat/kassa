package com.kassa.user.service

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

// IP 마다 최근 window 동안의 가입 시각을 들고 max 를 넘으면 거절한다
// 머신이 한 대라 메모리에 둔다. 재시작하면 비워진다
@Component
class SignupRateLimiter(
    @Value("\${app.signup-limit.max}") private val max: Int,
    @Value("\${app.signup-limit.window}") private val window: Duration,
    private val clock: Clock,
) {

    private val hits = ConcurrentHashMap<String, ArrayDeque<Instant>>()

    // 리턴은 이번 가입을 받아도 되는지. 받으면 시각을 남긴다
    fun tryAcquire(ip: String): Boolean {
        val now = clock.instant()
        val cutoff = now.minus(window)
        var accepted = false

        // 같은 IP 는 compute 가 한 번에 하나씩만 돌려서 동시에 몰려도 횟수가 새지 않는다
        hits.compute(ip) { _, old ->
            val times = old ?: ArrayDeque()
            while (times.isNotEmpty() && times.first() < cutoff) times.removeFirst()
            if (times.size < max) {
                times.addLast(now)
                accepted = true
            }
            times
        }

        // 다시 오지 않는 IP 가 쌓이니 맵이 커졌을 때만 창이 지난 IP 를 지운다
        if (hits.size > SWEEP_THRESHOLD) {
            hits.keys.forEach { key ->
                hits.computeIfPresent(key) { _, times -> if (times.last() < cutoff) null else times }
            }
        }

        return accepted
    }

    // 지금 들고 있는 IP 수
    internal fun trackedIps(): Int = hits.size

    companion object {
        const val SWEEP_THRESHOLD = 10_000
    }
}
