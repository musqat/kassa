package com.kassa

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class KassaApplication

fun main(args: Array<String>) {
	runApplication<KassaApplication>(*args)
}
