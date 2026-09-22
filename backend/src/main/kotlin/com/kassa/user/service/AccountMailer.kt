package com.kassa.user.service

import com.kassa.common.mail.EmailSender
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AccountMailer(
    private val emailSender: EmailSender,
    @Value("\${app.frontend-url}") private val frontendUrl: String,
) {

    fun sendLoginId(to: String, loginId: String) {
        emailSender.send(
            to = to,
            subject = "아이디 안내",
            body = """
                |이 이메일로 가입한 아이디는 $loginId 입니다.
                |
                |$frontendUrl/login
            """.trimMargin(),
        )
    }

    fun sendPasswordReset(to: String, rawToken: String) {
        emailSender.send(
            to = to,
            subject = "비밀번호 재설정",
            body = """
                |아래 링크에서 새 비밀번호를 정할 수 있습니다. 링크는 30분 동안 쓸 수 있습니다.
                |
                |$frontendUrl/reset-password?token=$rawToken
            """.trimMargin(),
        )
    }

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
