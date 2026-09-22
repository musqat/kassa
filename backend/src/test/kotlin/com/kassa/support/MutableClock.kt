package com.kassa.support

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

// 실제 현재 시각에서 출발한다
class MutableClock(private var instant: Instant = Instant.now()) : Clock() {

    fun advance(duration: Duration) {
        instant = instant.plus(duration)
    }

    fun reset() {
        instant = Instant.now()
    }

    override fun instant(): Instant = instant
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = this
}

@TestConfiguration(proxyBeanMethods = false)
class MutableClockConfig {

    @Bean
    @Primary
    fun mutableClock(): MutableClock = MutableClock()
}
