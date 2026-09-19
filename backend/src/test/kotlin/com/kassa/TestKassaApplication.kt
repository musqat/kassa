package com.kassa

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<KassaApplication>().with(TestcontainersConfiguration::class).run(*args)
}
