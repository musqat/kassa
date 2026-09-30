package com.kassa.user.service

import com.kassa.user.domain.User
import com.kassa.user.repository.UserRepository
import java.time.Clock
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

private val log = LoggerFactory.getLogger(AdminSeeder::class.java)

// 관리자 계정을 환경 변수로 만든다. 값이 없으면 만들지 않는다
@Component
class AdminSeeder(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock,
    @Value("\${app.admin.login-id:}") private val loginId: String,
    @Value("\${app.admin.email:}") private val email: String,
    @Value("\${app.admin.password:}") private val password: String,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        if (loginId.isBlank() || email.isBlank() || password.isBlank()) {
            return
        }

        // 이미 있으면 아무것도 하지 않는다. 비밀번호를 덮어쓰면 바꿔 둔 값이 되돌아간다
        val existing = userRepository.findByLoginId(loginId)
        if (existing != null) {
            log.info("관리자 계정이 이미 존재: {}", loginId)
            return
        }

        val admin = User(loginId, email, passwordEncoder.encode(password)!!, "관리자")
        admin.verifyEmail(Instant.now(clock))
        admin.promote()
        userRepository.save(admin)

        log.info("관리자 계정을 생성 완료 : {}", loginId)
    }
}
