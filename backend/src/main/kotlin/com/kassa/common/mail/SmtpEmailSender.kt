package com.kassa.common.mail

import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
class SmtpEmailSender(
    private val mailSender: JavaMailSender,
    @Value("\${app.mail.from}") private val from: String,
) : EmailSender {

    override fun send(to: String, subject: String, body: String) {
        val message = SimpleMailMessage()
        message.from = from
        message.setTo(to)
        message.subject = subject
        message.text = body
        mailSender.send(message)
    }
}
