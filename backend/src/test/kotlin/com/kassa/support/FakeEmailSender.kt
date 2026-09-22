package com.kassa.support

import com.kassa.common.mail.EmailSender
import org.springframework.mail.MailSendException
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

data class SentMail(val to: String, val subject: String, val body: String)

// 보낸 메일을 목록에 쌓는다
class FakeEmailSender : EmailSender {
    val sent = mutableListOf<SentMail>()

    // true 면 다음 발송 한 번을 SMTP 실패처럼 던진다
    var failNext = false

    override fun send(to: String, subject: String, body: String) {
        if (failNext) {
            failNext = false
            throw MailSendException("테스트용 발송 실패")
        }
        sent += SentMail(to, subject, body)
    }

    fun countTo(to: String): Int = sent.count { it.to == to }

    // 마지막 메일 본문의 링크에서 token= 뒤를 꺼낸다
    fun lastTokenTo(to: String): String {
        val mail = sent.last { it.to == to }
        return Regex("token=([A-Za-z0-9_-]+)").find(mail.body)!!.groupValues[1]
    }

    fun clear() {
        sent.clear()
        failNext = false
    }
}

@TestConfiguration(proxyBeanMethods = false)
class FakeEmailSenderConfig {

    @Bean
    @Primary
    fun fakeEmailSender(): FakeEmailSender = FakeEmailSender()
}
