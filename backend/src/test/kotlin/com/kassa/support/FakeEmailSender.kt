package com.kassa.support

import com.kassa.common.mail.EmailSender
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

data class SentMail(val to: String, val subject: String, val body: String)

// 보낸 메일을 목록에 쌓는다
class FakeEmailSender : EmailSender {
    val sent = mutableListOf<SentMail>()

    override fun send(to: String, subject: String, body: String) {
        sent += SentMail(to, subject, body)
    }

    fun clear() {
        sent.clear()
    }
}

@TestConfiguration(proxyBeanMethods = false)
class FakeEmailSenderConfig {

    @Bean
    @Primary
    fun fakeEmailSender(): FakeEmailSender = FakeEmailSender()
}
