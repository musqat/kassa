package com.kassa.user

import com.kassa.support.IntegrationTest
import com.kassa.user.domain.Role
import com.kassa.user.repository.UserRepository
import com.kassa.user.service.AdminSeeder
import java.time.Clock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.security.crypto.password.PasswordEncoder

class AdminSeederTest : IntegrationTest() {

    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var passwordEncoder: PasswordEncoder
    @Autowired private lateinit var clock: Clock

    /** 환경 변수로 받는 값이라 테스트에서는 직접 만들어 부른다 */
    private fun seeder(loginId: String, email: String, password: String) =
        AdminSeeder(userRepository, passwordEncoder, clock, loginId, email, password)

    private fun run(seeder: AdminSeeder) = seeder.run(DefaultApplicationArguments())

    @Test
    fun `값이 없으면 만들지 않는다`() {
        run(seeder("", "", ""))

        assertThat(userRepository.count()).isZero()
    }

    @Test
    fun `하나라도 비면 만들지 않는다`() {
        run(seeder("admin01", "admin@kassa.local", ""))

        assertThat(userRepository.count()).isZero()
    }

    @Test
    fun `값이 다 있으면 관리자를 만든다`() {
        run(seeder("admin01", "admin@kassa.local", "abcd1234"))

        val admin = userRepository.findByLoginId("admin01")!!

        assertThat(admin.role).isEqualTo(Role.ADMIN)
        // 인증 메일을 받을 수 없어서 인증된 상태로 저장한다
        assertThat(admin.isEmailVerified()).isTrue()
        assertThat(passwordEncoder.matches("abcd1234", admin.passwordHash)).isTrue()
    }

    @Test
    fun `두 번 돌려도 계정은 하나고 비밀번호도 그대로다`() {
        run(seeder("admin01", "admin@kassa.local", "abcd1234"))
        val first = userRepository.findByLoginId("admin01")!!.passwordHash

        // 재배포 때마다 시더가 돈다. 바꿔 둔 비밀번호를 되돌리면 안 된다
        run(seeder("admin01", "admin@kassa.local", "wxyz9999"))

        assertThat(userRepository.count()).isEqualTo(1)
        assertThat(userRepository.findByLoginId("admin01")!!.passwordHash).isEqualTo(first)
    }
}
