package com.kassa.order.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class OrderNoGeneratorTest {

    private val generator = OrderNoGenerator()

    @Test
    fun `날짜는 한국 시간 기준이다`() {
        // UTC 로 9월 23일 15시는 한국에서 9월 24일 0시다
        val orderNo = generator.generate(Instant.parse("2026-09-23T15:00:00Z"))

        assertThat(orderNo).startsWith("20260924-")
    }

    @Test
    fun `형식은 날짜 8자리와 무작위 8자리다`() {
        val orderNo = generator.generate(Instant.parse("2026-09-24T01:00:00Z"))

        assertThat(orderNo).matches("\\d{8}-[23456789A-HJ-NP-Z]{8}")
    }

    @Test
    fun `100번 만들어도 겹치지 않는다`() {
        val now = Instant.parse("2026-09-24T01:00:00Z")

        val generated = (1..100).map { generator.generate(now) }.toSet()

        assertThat(generated).hasSize(100)
    }
}
