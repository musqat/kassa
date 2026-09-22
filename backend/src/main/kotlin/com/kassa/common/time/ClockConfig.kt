package com.kassa.common.time

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

// 현재 시각은 이 빈으로 얻는다. 테스트에서 고정 시계로 바꿔 끼운다
@Configuration
class ClockConfig {

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}
