package com.kassa.user.service

import com.kassa.common.mail.EmailSender
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AccountMailer(
    private val emailSender: EmailSender,
    @Value("\${app.frontend-url}") private val frontendUrl: String,
) {

    fun sendVerification(to: String, rawToken: String) {
        emailSender.send(
            to = to,
            subject = "[계산대] 이메일 인증",
            body = """
                |아래 링크를 열면 가입이 끝납니다. 링크는 24시간 동안 쓸 수 있습니다.
                |
                |$frontendUrl/verify-email?token=$rawToken
                |
            """.trimMargin(),
        )
    }

}
