package com.kassa.common.security

import org.springframework.security.oauth2.jwt.Jwt

// 회원 id 는 토큰에서만 꺼낸다. 요청 본문·경로로 받지 않는다
fun Jwt.userId(): Long = subject!!.toLong()
