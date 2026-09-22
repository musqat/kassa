package com.kassa.support

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

// 실제 현재 시각에서 출발한다. DB(TIMESTAMPTZ)와 같은 마이크로초 단위로 자른다
class MutableClock(private var instant: Instant = microsNow()) : Clock() {

    fun advance(duration: Duration) {
        instant = instant.plus(duration)
    }

    fun reset() {
        instant = microsNow()
    }

    override fun instant(): Instant = instant
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
}

private fun microsNow(): Instant = Instant.now().truncatedTo(ChronoUnit.MICROS)

@TestConfiguration(proxyBeanMethods = false)
class MutableClockConfig {

    @Bean
    @Primary
    fun mutableClock(): MutableClock = MutableClock()
}
