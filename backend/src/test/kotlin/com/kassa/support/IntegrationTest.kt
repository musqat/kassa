package com.kassa.support

import com.kassa.TestcontainersConfiguration
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

// Testcontainers 로 PostgreSQL 을 띄운다
@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration::class, FakeEmailSenderConfig::class, DatabaseCleaner::class)
abstract class IntegrationTest {

    @Autowired
    private lateinit var databaseCleaner: DatabaseCleaner

    // 하위 클래스의 @BeforeEach 보다 먼저 돈다
    @BeforeEach
    fun clearDatabase() {
        databaseCleaner.clear()
    }
}
