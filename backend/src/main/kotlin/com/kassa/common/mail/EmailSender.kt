package com.kassa.common.mail

interface EmailSender {
    fun send(to: String, subject: String, body: String)
}
